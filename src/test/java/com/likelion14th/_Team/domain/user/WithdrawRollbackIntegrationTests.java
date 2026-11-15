package com.likelion14th._Team.domain.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

import com.likelion14th._Team.domain.user.WithdrawFixture.Scenario;
import com.likelion14th._Team.domain.user.repository.UserProfileRepository;
import com.likelion14th._Team.domain.user.service.WithdrawService;
import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 격리된 테스트 MySQL에서 데이터를 실제로 커밋하고, 탈퇴 실패 시 전체 롤백을 검증한다.
 * 정리는 이번 테스트가 생성한 Scenario의 ID만 대상으로 한다.
 */
@ActiveProfiles("test")
@SpringBootTest
class WithdrawRollbackIntegrationTests {

    @Autowired EntityManager em;
    @Autowired WithdrawService withdrawService;
    @Autowired PlatformTransactionManager txManager;
    @MockitoSpyBean UserProfileRepository userProfileRepository;
    private Scenario scenario;

    private TransactionTemplate tx;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(txManager);
    }

    @AfterEach
    void tearDown() {
        reset(userProfileRepository);
        if (scenario != null) cleanup();
    }

    @Test
    void failureMidWayRollsBackEverythingAndRetrySucceeds() {
        scenario = tx.execute(st -> new WithdrawFixture(em).create());
        Scenario s = scenario;
        List<String> before = tx.execute(st -> new WithdrawFixture(em).nonEmptyTables(s));
        assertTrue(before.size() >= 11, "탈퇴 전에는 모든 테이블에 내 데이터가 있어야 한다: " + before);

        // 탈퇴 마지막 단계(user_profile 삭제)에서 실패시킨다. 그 앞의 삭제들은 이미 실행된 뒤다
        doThrow(new IllegalStateException("강제 실패")).when(userProfileRepository).deleteByUserId(anyLong());

        CustomException e = assertThrows(CustomException.class, () -> withdrawService.withdraw(s.userId()));
        assertEquals(ErrorCode.INTERNAL_ERROR, e.getErrorCode());

        // 이미 지운 수강계획·귀국보고서·북마크 등도 전부 되돌아와야 한다
        List<String> after = tx.execute(st -> new WithdrawFixture(em).nonEmptyTables(s));
        assertEquals(before, after, "실패하면 이미 지운 데이터도 모두 되돌아와야 한다");
        Long sourceCourseId = tx.execute(st -> new WithdrawFixture(em).sourceCourseIdOf(s.otherItemId()));
        assertEquals(s.courseId(), sourceCourseId, "다른 회원 항목의 source_course_id NULL 처리도 되돌아와야 한다");

        // 실패 설정을 풀고 다시 탈퇴하면 정상 완료된다 (잠금이나 부분 삭제가 남지 않았다는 뜻)
        reset(userProfileRepository);
        withdrawService.withdraw(s.userId());
        List<String> remaining = tx.execute(st -> new WithdrawFixture(em).nonEmptyTables(s));
        assertEquals(List.of(), remaining);
    }

    /** 이미 탈퇴한 회원은 건너뛰고, 이 실행에서 생성한 두 회원과 대학만 정리한다. */
    private void cleanup() {
        for (Long userId : List.of(scenario.userId(), scenario.otherUserId())) {
            boolean exists = Boolean.TRUE.equals(tx.execute(st -> em.createQuery(
                            "select count(u) from User u where u.id = :id", Long.class)
                    .setParameter("id", userId).getSingleResult() > 0));
            if (exists) withdrawService.withdraw(userId);
        }
        tx.executeWithoutResult(st -> em.createQuery("delete from University u where u.id = :id")
                .setParameter("id", scenario.universityId()).executeUpdate());
        Long leftoverUsers = tx.execute(st -> em.createQuery(
                        "select count(u) from User u where u.id in :ids", Long.class)
                .setParameter("ids", List.of(scenario.userId(), scenario.otherUserId())).getSingleResult());
        Long leftoverUniversities = tx.execute(st -> em.createQuery(
                        "select count(u) from University u where u.id = :id", Long.class)
                .setParameter("id", scenario.universityId()).getSingleResult());
        assertEquals(0L, leftoverUsers, "이 테스트의 회원 데이터가 DB에 남았다");
        assertEquals(0L, leftoverUniversities, "이 테스트의 대학 데이터가 DB에 남았다");
    }
}
