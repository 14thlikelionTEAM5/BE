package com.likelion14th._Team.global.security;

import com.likelion14th._Team.global.exception.ErrorCode;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.Filter;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AuthenticationRoutingTests {
    @Autowired
    WebApplicationContext context;
    @Autowired
    @Qualifier("springSecurityFilterChain")
    Filter security;
    @Value("${jwt.secret}")
    String signingKey;
    private MockMvc mvc;
    private String expired;
    private String forged;
    private record Endpoint(HttpMethod method, String path, ErrorCode error) {}

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
        Instant now = Instant.now();
        expired = Jwts.builder().subject("1").issuedAt(Date.from(now.minusSeconds(120)))
                .expiration(Date.from(now.minusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(signingKey.getBytes(StandardCharsets.UTF_8))).compact();
        byte[] otherKey = new byte[32];
        new SecureRandom().nextBytes(otherKey);
        forged = Jwts.builder().subject("1").expiration(Date.from(now.plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(otherKey)).compact();
    }

    private List<Endpoint> endpoints() {
        return List.of(
                new Endpoint(HttpMethod.GET, "/api/v1/universities", ErrorCode.UNIVERSITY_LOGIN_REQUIRED),
                new Endpoint(HttpMethod.GET, "/api/v1/universities/countries", ErrorCode.UNIVERSITY_COUNTRIES_LOGIN_REQUIRED),
                new Endpoint(HttpMethod.GET, "/api/v1/universities/1", ErrorCode.UNIVERSITY_COUNTRIES_LOGIN_REQUIRED),
                // /search는 기존 상세 패턴 /universities/{universityId}에도 매칭된다.
                // 매처 순서는 유지하되, 인증 필요 메시지는 모든 보호 경로에서 공통 문구로 검증한다.
                new Endpoint(HttpMethod.GET, "/api/v1/universities/search", ErrorCode.UNIVERSITY_COUNTRIES_LOGIN_REQUIRED),
                new Endpoint(HttpMethod.GET, "/api/v1/universities/1/admission-reports", ErrorCode.UNAUTHORIZED),
                new Endpoint(HttpMethod.GET, "/api/v1/universities/1/admission-reports/stats", ErrorCode.UNAUTHORIZED),
                new Endpoint(HttpMethod.POST, "/api/v1/return-reports", ErrorCode.UNAUTHORIZED),
                new Endpoint(HttpMethod.POST, "/api/v1/return-reports/extract", ErrorCode.UNAUTHORIZED),
                new Endpoint(HttpMethod.PUT, "/api/v1/return-reports/1", ErrorCode.UNAUTHORIZED),
                new Endpoint(HttpMethod.DELETE, "/api/v1/return-reports/1", ErrorCode.UNAUTHORIZED),
                // 아직 매핑되지 않은 하위 경로도 /** 인증 규칙으로 보호되는지 확인한다.
                new Endpoint(HttpMethod.GET, "/api/v1/return-reports/1/unmapped", ErrorCode.UNAUTHORIZED),
                new Endpoint(HttpMethod.GET, "/api/v1/home-courses", ErrorCode.UNAUTHORIZED),
                new Endpoint(HttpMethod.POST, "/api/v1/consents", ErrorCode.UNAUTHORIZED),
                new Endpoint(HttpMethod.GET, "/api/v1/study-plans", ErrorCode.AUTHENTICATION_REQUIRED),
                new Endpoint(HttpMethod.POST, "/api/v1/study-plans", ErrorCode.AUTHENTICATION_REQUIRED),
                new Endpoint(HttpMethod.GET, "/api/v1/study-plans/1", ErrorCode.STUDY_PLAN_LOGIN_REQUIRED),
                new Endpoint(HttpMethod.PATCH, "/api/v1/study-plans/1", ErrorCode.STUDY_PLAN_LOGIN_REQUIRED),
                new Endpoint(HttpMethod.DELETE, "/api/v1/study-plans/1", ErrorCode.STUDY_PLAN_LOGIN_REQUIRED),
                new Endpoint(HttpMethod.PATCH, "/api/v1/study-plans", ErrorCode.STUDY_PLAN_LOGIN_REQUIRED),
                new Endpoint(HttpMethod.DELETE, "/api/v1/study-plans", ErrorCode.STUDY_PLAN_LOGIN_REQUIRED),
                new Endpoint(HttpMethod.POST, "/api/v1/study-plans/1/items", ErrorCode.STUDY_PLAN_LOGIN_REQUIRED),
                new Endpoint(HttpMethod.PATCH, "/api/v1/study-plans/1/items/2", ErrorCode.STUDY_PLAN_LOGIN_REQUIRED),
                new Endpoint(HttpMethod.DELETE, "/api/v1/study-plans/1/items/2", ErrorCode.STUDY_PLAN_LOGIN_REQUIRED),
                new Endpoint(HttpMethod.DELETE, "/api/v1/study-plans/1/items", ErrorCode.STUDY_PLAN_LOGIN_REQUIRED));
    }

    private void assertUnauthorized(String token) throws Exception {
        for (Endpoint endpoint : endpoints()) {
            MockHttpServletRequestBuilder request = request(endpoint.method(), endpoint.path());
            if (token != null) {
                request.header("Authorization", "Bearer " + token);
            }
            mvc.perform(request)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(endpoint.error().getCode()))
                    .andExpect(jsonPath("$.message").value("로그인이 필요합니다."))
                    .andExpect(jsonPath("$.data").doesNotExist())
                    .andExpect(jsonPath("$.errors").doesNotExist())
                    .andExpect(jsonPath("$.timestamp").isNotEmpty());
        }
    }

    @Test
    void missingTokensReturnUnified401Message() throws Exception {
        assertUnauthorized(null);
    }

    @Test
    void expiredTokensReturnUnified401Message() throws Exception {
        assertUnauthorized(expired);
    }

    @Test
    void forgedTokensReturnUnified401Message() throws Exception {
        assertUnauthorized(forged);
    }

    @Test
    void publicPathsAndPreflightRemainAccessible() throws Exception {
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/login")).andExpect(status().isBadRequest());
        mvc.perform(options("/api/v1/study-plans")).andExpect(status().isOk());
        mvc.perform(get("/security-test-unmapped")).andExpect(status().isNotFound());
    }
}
