package com.likelion14th._Team.domain.bookmark;

import static org.junit.jupiter.api.Assertions.*;

import com.likelion14th._Team.domain.bookmark.repository.BookmarkRepository;
import com.likelion14th._Team.domain.bookmark.service.BookmarkService;
import com.likelion14th._Team.domain.university.entity.University;
import com.likelion14th._Team.domain.university.repository.UniversityRepository;
import com.likelion14th._Team.domain.user.entity.User;
import com.likelion14th._Team.domain.user.repository.UserRepository;
import com.likelion14th._Team.global.enums.MajorType;
import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
class BookmarkConcurrencyTests {
    @Autowired BookmarkService service;
    @Autowired BookmarkRepository bookmarks;
    @Autowired UserRepository users;
    @Autowired UniversityRepository schools;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactions;

    @Test
    void simultaneousRequestsStoreOnlyOneBookmark() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactions);
        long[] ids = tx.execute(status -> {
            String key = UUID.randomUUID().toString().replace("-", "");
            User user = users.saveAndFlush(User.builder().schoolEmail(key + "@example.hufs.ac.kr")
                    .nickname(key.substring(0, 12)).password(UUID.randomUUID().toString())
                    .majorType(MajorType.PRIMARY).build());
            University school = schools.saveAndFlush(University.builder()
                    .name("bookmark-concurrent-" + key).country("테스트").build());
            return new long[]{user.getId(), school.getId()};
        });
        var pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        java.util.concurrent.Callable<Integer> call = () -> {
            ready.countDown();
            if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("start timeout");
            try {
                assertTrue(service.create(ids[0], ids[1]).isBookmarked());
                return 201;
            } catch (CustomException exception) {
                assertEquals(ErrorCode.BOOKMARK_DUPLICATED, exception.getErrorCode());
                return 409;
            }
        };
        try {
            var first = pool.submit(call);
            var second = pool.submit(call);
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            var statuses = List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS))
                    .stream().sorted().toList();
            assertEquals(List.of(201, 409), statuses);
            tx.executeWithoutResult(status -> {
                Number count = (Number) em.createNativeQuery(
                        "select count(*) from bookmark where user_id=:user and university_id=:school")
                        .setParameter("user", ids[0]).setParameter("school", ids[1]).getSingleResult();
                assertEquals(1, count.intValue());
            });

            CountDownLatch deleteStart = new CountDownLatch(1);
            java.util.concurrent.Callable<Integer> remove = () -> {
                if (!deleteStart.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("delete timeout");
                try {
                    assertFalse(service.delete(ids[0], ids[1]).isBookmarked());
                    return 200;
                } catch (CustomException exception) {
                    assertEquals(ErrorCode.BOOKMARK_NOT_FOUND, exception.getErrorCode());
                    return 404;
                }
            };
            var firstDelete = pool.submit(remove);
            var secondDelete = pool.submit(remove);
            deleteStart.countDown();
            assertEquals(List.of(200, 404), List.of(firstDelete.get(15, TimeUnit.SECONDS),
                    secondDelete.get(15, TimeUnit.SECONDS)).stream().sorted().toList());
            assertFalse(bookmarks.existsByUserIdAndUniversityId(ids[0], ids[1]));
        } finally {
            start.countDown();
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(20, TimeUnit.SECONDS));
            tx.executeWithoutResult(status -> {
                em.createNativeQuery("delete from bookmark where user_id=:id")
                        .setParameter("id", ids[0]).executeUpdate();
                users.deleteById(ids[0]);
                schools.deleteById(ids[1]);
            });
        }
    }
}
