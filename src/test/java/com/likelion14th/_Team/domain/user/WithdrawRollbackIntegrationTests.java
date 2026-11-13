package com.likelion14th._Team.domain.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 탈퇴 도중 실패하면 이미 지운 데이터까지 전부 되돌아오는지(전체 롤백) 검증한다.
 *
 * 롤백은 테스트가 트랜잭션을 감싸면 확인할 수 없어서 데이터를 실제로 커밋한다.
 * 그래서 개발 DB를 지키는 장치를 둔다.
 *  1) 접속 DB가 localhost일 때만 실행한다 (DB_URL 환경변수로 원격 DB를 가리키면 건너뜀)
 *  2) 모든 데이터는 "withdraw-it-" 접두어를 쓴다
 *  3) 시작 전·종료 후에 접두어 데이터를 청소하고, 남아 있으면 테스트를 실패시킨다
 */
@SpringBootTest
class WithdrawRollbackIntegrationTests {

    @Autowired EntityManager em;
    @Autowired WithdrawService withdrawService;
    @Autowired PlatformTransactionManager txManager;
    @MockitoSpyBean UserProfileRepository userProfileRepository;
    @Value("${spring.datasource.url}") String datasourceUrl;

    private TransactionTemplate tx;

    @BeforeEach
    void setUp() {
        assumeTrue(isLocalDb(), "접속 DB가 localhost가 아니라서 건너뜀: " + datasourceUrl);
        tx = new TransactionTemplate(txManager);
        cleanup(); // 이전 실행이 중간에 죽어서 남긴 데이터가 있으면 먼저 치운다
    }

    @AfterEach
    void tearDown() {
        if (!isLocalDb()) {
            return;
        }
        cleanup();
    }

    @Test
    void failureMidWayRollsBackEverythingAndRetrySucceeds() {
        Scenario s = tx.execute(st -> new WithdrawFixture(em).create());
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

    private boolean isLocalDb() {
        return datasourceUrl.contains("localhost") || datasourceUrl.contains("127.0.0.1");
    }

    /** 접두어가 붙은 테스트 데이터를 모두 지우고, 남아 있으면 실패시킨다 */
    private void cleanup() {
        reset(userProfileRepository); // 강제 실패 설정이 남아 있으면 청소가 막히므로 먼저 푼다
        String like = WithdrawFixture.PREFIX + "%";

        List<Long> userIds = tx.execute(st -> em.createQuery(
                        "select u.id from User u where u.schoolEmail like :p", Long.class)
                .setParameter("p", like).getResultList());
        for (Long userId : userIds) {
            withdrawService.withdraw(userId); // 운영 탈퇴 로직으로 회원과 연결 데이터를 지운다
        }
        tx.executeWithoutResult(st -> em.createQuery("delete from University u where u.name like :p")
                .setParameter("p", like).executeUpdate());

        Long leftoverUsers = tx.execute(st -> em.createQuery(
                        "select count(u) from User u where u.schoolEmail like :p", Long.class)
                .setParameter("p", like).getSingleResult());
        Long leftoverUniversities = tx.execute(st -> em.createQuery(
                        "select count(u) from University u where u.name like :p", Long.class)
                .setParameter("p", like).getSingleResult());
        assertEquals(0L, leftoverUsers, "테스트 회원 데이터가 DB에 남았다");
        assertEquals(0L, leftoverUniversities, "테스트 대학 데이터가 DB에 남았다");
    }
}