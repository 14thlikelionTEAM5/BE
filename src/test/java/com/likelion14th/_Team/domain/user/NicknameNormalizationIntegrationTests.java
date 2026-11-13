package com.likelion14th._Team.domain.user;

import com.likelion14th._Team.domain.user.entity.EmailVerification;
import com.likelion14th._Team.domain.user.repository.UserRepository;
import com.likelion14th._Team.global.security.JwtProvider;
import jakarta.persistence.EntityManager;
import jakarta.servlet.Filter;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class NicknameNormalizationIntegrationTests {
    @Autowired EntityManager em;
    @Autowired UserRepository users;
    @Autowired JwtProvider jwt;
    @Autowired WebApplicationContext context;
    @Autowired @Qualifier("springSecurityFilterChain") Filter security;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
    }

    private String key() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String nfd(String nickname) {
        return Normalizer.normalize(nickname, Normalizer.Form.NFD);
    }

    private String verifiedEmail() {
        String email = key() + "@hufs.ac.kr";
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        em.persist(EmailVerification.builder().schoolEmail(email).codeHash("test-only")
                .verifiedAt(now).expiresAt(now.plusMinutes(10)).lastSentAt(now).build());
        em.flush();
        return email;
    }

    private String signupBody(String email, String nickname) {
        return """
                {"schoolEmail":"%s","password":"test-password","nickname":"%s",
                 "majorType":"PRIMARY","gpa":4.0,
                 "consents":{"termsOfService":true,"privacyPolicy":true},
                 "languageScores":[{"testType":"IELTS","score":7.0}]}
                """.formatted(email, nickname);
    }

    private Long signup(String email, String nickname, String expected) throws Exception {
        mvc.perform(post("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody(email, nickname)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.nickname").value(expected));
        em.flush();
        em.clear();
        var user = users.findBySchoolEmail(email).orElseThrow();
        assertEquals(expected, user.getNickname());
        assertTrue(Normalizer.isNormalized(user.getNickname(), Normalizer.Form.NFC));
        return user.getId();
    }

    @Test
    void nfdCheckSignupAndUpdateUseTheSameStoredNickname() throws Exception {
        String nickname = "채원" + key().substring(0, 8);
        String input = nfd(nickname);
        mvc.perform(get("/api/v1/users/nickname/check").param("nickname", input))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.available").value(true))
                .andExpect(jsonPath("$.data.nickname").value(input));
        String email = verifiedEmail();
        Long id = signup(email, input, nickname);
        String bearer = "Bearer " + jwt.createAccessToken(id);
        mvc.perform(get("/api/v1/users/nickname/check").param("nickname", input))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.available").value(false));
        mvc.perform(get("/api/v1/users/nickname/check").param("nickname", nickname)
                        .header("Authorization", bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.isCurrent").value(true));

        String renamed = "수정" + key().substring(0, 8);
        mvc.perform(patch("/api/v1/users/me").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nickname\":\"" + nfd(renamed) + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.nickname").value(renamed));
        em.flush();
        em.clear();
        assertEquals(renamed, users.findById(id).orElseThrow().getNickname());
        mvc.perform(get("/api/v1/users/nickname/check").param("nickname", nfd(renamed))
                        .header("Authorization", bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.available").value(true))
                .andExpect(jsonPath("$.data.isCurrent").value(true));
        mvc.perform(get("/api/v1/users/nickname/check").param("nickname", input))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.available").value(true));

        for (String body : new String[]{"{}", "{\"nickname\":null}"}) {
            mvc.perform(patch("/api/v1/users/me").header("Authorization", bearer)
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.nickname").value(renamed))
                    .andExpect(jsonPath("$.data.languageScores[0].testType").value("IELTS"));
        }
    }

    @Test
    void normalizedDuplicateAndCaseOnlyOwnUpdateBehaveConsistently() throws Exception {
        String suffix = key().substring(0, 6);
        String ownerName = "한A" + suffix;
        Long ownerId = signup(verifiedEmail(), ownerName, ownerName);
        String candidate = "한a" + suffix;
        mvc.perform(get("/api/v1/users/nickname/check").param("nickname", nfd(candidate)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.available").value(false));
        mvc.perform(post("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody(verifiedEmail(), nfd(candidate))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("E409"));
        String otherName = "다른" + key().substring(0, 8);
        Long otherId = signup(verifiedEmail(), otherName, otherName);
        mvc.perform(patch("/api/v1/users/me").header("Authorization", "Bearer " + jwt.createAccessToken(otherId))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nickname\":\"" + nfd(candidate) + "\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("E409"));
        assertEquals(otherName, users.findById(otherId).orElseThrow().getNickname());
        mvc.perform(patch("/api/v1/users/me").header("Authorization", "Bearer " + jwt.createAccessToken(ownerId))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nickname\":\"" + nfd(candidate) + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.nickname").value(candidate));
        em.flush();
        em.clear();
        assertEquals(candidate, users.findById(ownerId).orElseThrow().getNickname());
    }

    @Test
    void invalidNormalizedNicknameStillHasFieldErrorInAllThreeApis() throws Exception {
        String nickname = "테스트" + key().substring(0, 6);
        Long id = signup(verifiedEmail(), nickname, nickname);
        String bearer = "Bearer " + jwt.createAccessToken(id);
        for (String invalid : new String[]{"가", "가".repeat(13), "ㅋㅋ", "채 원", "😀😀"}) {
            String input = nfd(invalid);
            mvc.perform(get("/api/v1/users/nickname/check").param("nickname", input))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors[0].field").value("nickname"));
            mvc.perform(post("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON)
                            .content(signupBody("notused@hufs.ac.kr", input)))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors[0].field").value("nickname"));
            mvc.perform(patch("/api/v1/users/me").header("Authorization", bearer)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"nickname\":\"" + input + "\"}"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors[0].field").value("nickname"));
        }
    }
}
