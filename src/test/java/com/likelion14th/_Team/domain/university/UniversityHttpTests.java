package com.likelion14th._Team.domain.university;

import com.likelion14th._Team.domain.user.entity.User;
import com.likelion14th._Team.domain.user.repository.UserRepository;
import com.likelion14th._Team.global.enums.MajorType;
import com.likelion14th._Team.global.security.JwtProvider;
import jakarta.servlet.Filter;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class UniversityHttpTests {
    @Autowired WebApplicationContext context;
    @Autowired @Qualifier("springSecurityFilterChain") Filter security;
    @Autowired JwtProvider jwt;
    @Autowired UserRepository users;
    private Long userId;
    private MockMvc mvc;
    @BeforeEach void setup() {
        mvc=MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
        String key = UUID.randomUUID().toString().replace("-", "");
        userId = users.saveAndFlush(User.builder().schoolEmail(key + "@example.hufs.ac.kr")
                .nickname(key.substring(0, 12)).password(UUID.randomUUID().toString())
                .majorType(MajorType.PRIMARY).build()).getId();
    }
    @Test void allThreeRoutesRequireAuthentication() throws Exception {
        for (String path : new String[]{"/api/v1/universities", "/api/v1/universities/countries", "/api/v1/universities/1"}) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("E401"));
            mvc.perform(get(path).header("Authorization", "Bearer invalid"))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("E401"));
        }
    }
    @Test void invalidSearchAndPathReturn400() throws Exception {
        String token=jwt.createAccessToken(userId);
        mvc.perform(get("/api/v1/universities").param("sort", "WRONG").header("Authorization", "Bearer "+token))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("검색 조건이 올바르지 않습니다."));
        mvc.perform(get("/api/v1/universities/0").header("Authorization", "Bearer "+token))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors[0].field").value("universityId"));
        mvc.perform(get("/api/v1/universities/1").param("semester", "2027-3").header("Authorization", "Bearer "+token))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors[0].field").value("semester"));
    }
}
