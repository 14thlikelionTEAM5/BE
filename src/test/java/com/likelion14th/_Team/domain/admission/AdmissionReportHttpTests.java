package com.likelion14th._Team.domain.admission;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class AdmissionReportHttpTests {
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
    @Test void statsAndListRequireValidAuthentication() throws Exception {
        for (String suffix : new String[]{"", "/stats"}) {
            String path="/api/v1/universities/1/admission-reports"+suffix;
            mvc.perform(get(path)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("E401"));
            mvc.perform(get(path).header("Authorization", "Bearer invalid"))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("E401"));
        }
    }
    @Test void invalidResultReturnsFieldErrorBeforeServiceCall() throws Exception {
        mvc.perform(get("/api/v1/universities/1/admission-reports").param("result", "WRONG")
                .header("Authorization", "Bearer "+jwt.createAccessToken(userId)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors[0].field").value("result"));
    }
}
