package com.likelion14th._Team.domain.user;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.likelion14th._Team.domain.user.WithdrawFixture.Scenario;
import com.likelion14th._Team.domain.user.entity.User;
import com.likelion14th._Team.domain.user.service.WithdrawService;
import com.likelion14th._Team.global.exception.ErrorCode;
import com.likelion14th._Team.global.security.JwtProvider;
import jakarta.persistence.EntityManager;
import jakarta.servlet.Filter;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * 실제 DB(MySQL)로 탈퇴 동작을 검증한다. 클래스 전체가 하나의 트랜잭션이라 끝나면 롤백된다.
 * (중간 실패 시 전체 롤백 검증은 커밋이 필요해서 WithdrawRollbackIntegrationTests에서 한다)
 */
@SpringBootTest
@Transactional
class WithdrawIntegrationTests {

    private static final String COUNTRIES_URL = "/api/v1/universities/countries";
    private static final String ME_URL = "/api/v1/users/me";

    @Autowired EntityManager em;
    @Autowired WithdrawService withdrawService;
    @Autowired JwtProvider jwtProvider;
    @Autowired WebApplicationContext context;
    @Autowired @Qualifier("springSecurityFilterChain") Filter security;

    private WithdrawFixture fixture;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        fixture = new WithdrawFixture(em);
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
    }

    /** 데이터를 DB에 반영하고 영속성 컨텍스트를 비워서, 이후 조회가 항상 DB를 보게 한다 */
    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    @Test
    void withdrawDeletesAllOfMyDataAndKeepsOtherUsers() {
        Scenario s = fixture.create();
        flushAndClear();
        assertTrue(fixture.nonEmptyTables(s).size() >= 11, "탈퇴 전에는 모든 테이블에 내 데이터가 있어야 한다");

        withdrawService.withdraw(s.userId());
        flushAndClear();

        assertEquals(List.of(), fixture.nonEmptyTables(s));
        assertNotNull(em.find(User.class, s.otherUserId()), "다른 회원은 그대로여야 한다");
    }

    @Test
    void otherUsersPlanItemIsKeptButLosesSourceCourse() {
        Scenario s = fixture.create();
        flushAndClear();
        assertEquals(s.courseId(), fixture.sourceCourseIdOf(s.otherItemId()));

        withdrawService.withdraw(s.userId());
        flushAndClear();

        assertTrue(fixture.itemExists(s.otherItemId()), "다른 회원의 수강계획 항목은 삭제되면 안 된다");
        assertNull(fixture.sourceCourseIdOf(s.otherItemId()), "내 과목 참조(source_course_id)만 NULL이 돼야 한다");
    }

    @Test
    void sameEmailAndNicknameCanSignUpAgainAfterWithdraw() {
        Scenario s = fixture.create();
        flushAndClear();

        withdrawService.withdraw(s.userId());
        flushAndClear();

        // 메일·닉네임의 UNIQUE 제약 때문에 흔적이 남아 있으면 여기서 예외가 난다
        Long newUserId = assertDoesNotThrow(() -> fixture.signUp(s.email(), s.nickname()));
        assertNotEquals(s.userId(), newUserId);
    }

    @Test
    void tokenIssuedBeforeWithdrawIsRejectedAfterwards() throws Exception {
        Scenario s = fixture.create();
        flushAndClear();
        String bearer = "Bearer " + jwtProvider.createAccessToken(s.userId());

        // 탈퇴 전: 같은 토큰으로 사용자 조회가 없는 보호 API에 접근할 수 있다
        int before = mvc.perform(get(COUNTRIES_URL).header("Authorization", bearer))
                .andReturn().getResponse().getStatus();
        assertNotEquals(401, before);

        mvc.perform(delete(ME_URL).header("Authorization", bearer)).andExpect(status().isOk());

        // 탈퇴 후: 같은 토큰은 사용자 조회가 없는 보호 API에서도 401이다
        mvc.perform(get(COUNTRIES_URL).header("Authorization", bearer))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.UNIVERSITY_COUNTRIES_LOGIN_REQUIRED.getCode()));
    }
}