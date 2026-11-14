package com.likelion14th._Team.domain.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

import com.likelion14th._Team.domain.bookmark.entity.Bookmark;
import com.likelion14th._Team.domain.bookmark.repository.BookmarkRepository;
import com.likelion14th._Team.domain.university.entity.University;
import com.likelion14th._Team.domain.university.repository.UniversityRepository;
import com.likelion14th._Team.domain.user.entity.User;
import com.likelion14th._Team.domain.user.repository.ConsentRepository;
import com.likelion14th._Team.domain.user.repository.UserRepository;
import com.likelion14th._Team.domain.user.service.WithdrawService;
import com.likelion14th._Team.global.enums.MajorType;
import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** 실제 커밋과 롤백을 관찰하므로 테스트 자체에는 @Transactional을 붙이지 않는다. */
@ActiveProfiles("test")
@SpringBootTest
class WithdrawDatabaseTests {
    @Autowired WithdrawService service;
    @Autowired UserRepository users;
    @Autowired UniversityRepository universities;
    @Autowired BookmarkRepository bookmarks;
    @Autowired PlatformTransactionManager transactions;
    @MockitoSpyBean ConsentRepository consents;
    private Long userId;
    private Long universityId;

    @BeforeEach
    void commitFixture() {
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            String key = UUID.randomUUID().toString().replace("-", "");
            User user = users.saveAndFlush(User.builder().schoolEmail(key + "@example.hufs.ac.kr")
                    .nickname(key.substring(0, 12)).password(UUID.randomUUID().toString())
                    .majorType(MajorType.PRIMARY).build());
            University university = universities.saveAndFlush(University.builder()
                    .name("withdraw-isolation-" + key).country("테스트").build());
            bookmarks.saveAndFlush(Bookmark.builder().user(user).university(university).build());
            userId = user.getId();
            universityId = university.getId();
        });
    }

    @AfterEach
    void removeOnlyThisTestsFixture() {
        reset(consents);
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            bookmarks.deleteForUser(userId, universityId);
            users.findById(userId).ifPresent(users::delete);
            universities.findById(universityId).ifPresent(universities::delete);
        });
        assertFalse(users.existsById(userId));
        assertFalse(universities.existsById(universityId));
        assertFalse(bookmarks.existsByUserIdAndUniversityId(userId, universityId));
    }

    @Test
    void successfulWithdrawalCommitsDeletionWithoutDeletingUniversity() {
        service.withdraw(userId);
        assertFalse(users.existsById(userId));
        assertFalse(bookmarks.existsByUserIdAndUniversityId(userId, universityId));
        assertTrue(universities.existsById(universityId));
    }

    @Test
    void failureAfterBookmarkDeletionRollsBackTheEntireWithdrawal() {
        doThrow(new DataAccessResourceFailureException("isolated test failure"))
                .when(consents).deleteAllByUserId(userId);
        CustomException error = assertThrows(CustomException.class, () -> service.withdraw(userId));
        assertEquals(ErrorCode.INTERNAL_ERROR, error.getErrorCode());
        assertTrue(users.existsById(userId));
        assertTrue(bookmarks.existsByUserIdAndUniversityId(userId, universityId));
    }
}
