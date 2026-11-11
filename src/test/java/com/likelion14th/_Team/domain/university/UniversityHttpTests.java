package com.likelion14th._Team.domain.university;

import com.likelion14th._Team.global.security.JwtProvider;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class UniversityHttpTests {
    @Autowired WebApplicationContext context;
    @Autowired @Qualifier("springSecurityFilterChain") Filter security;
    @Autowired JwtProvider jwt;
    private MockMvc mvc;
    @BeforeEach void setup() { mvc=MockMvcBuilders.webAppContextSetup(context).addFilters(security).build(); }
    @Test void allThreeRoutesRequireAuthentication() throws Exception {
        for (String path : new String[]{"/api/v1/universities", "/api/v1/universities/countries", "/api/v1/universities/1"}) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("E401"));
            mvc.perform(get(path).header("Authorization", "Bearer invalid"))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("E401"));
        }
    }
    @Test void invalidSearchAndPathReturn400() throws Exception {
        String token=jwt.createAccessToken(1L);
        mvc.perform(get("/api/v1/universities").param("sort", "WRONG").header("Authorization", "Bearer "+token))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("검색 조건이 올바르지 않습니다."));
        mvc.perform(get("/api/v1/universities/0").header("Authorization", "Bearer "+token))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors[0].field").value("universityId"));
        mvc.perform(get("/api/v1/universities/1").param("semester", "2027-3").header("Authorization", "Bearer "+token))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors[0].field").value("semester"));
    }
}
