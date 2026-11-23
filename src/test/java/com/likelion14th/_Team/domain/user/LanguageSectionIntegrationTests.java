package com.likelion14th._Team.domain.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.likelion14th._Team.domain.auth.dto.SignupRequest;
import com.likelion14th._Team.domain.auth.service.AuthService;
import com.likelion14th._Team.domain.bookmark.entity.Bookmark;
import com.likelion14th._Team.domain.bookmark.service.BookmarkService;
import com.likelion14th._Team.domain.university.dto.UniversitySearch;
import com.likelion14th._Team.domain.university.entity.*;
import com.likelion14th._Team.domain.university.service.UniversityQueryService;
import com.likelion14th._Team.domain.user.entity.*;
import com.likelion14th._Team.domain.user.service.MyInfoService;
import com.likelion14th._Team.global.enums.*;
import com.likelion14th._Team.global.security.JwtProvider;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LanguageSectionIntegrationTests {
    @Autowired EntityManager em;
    @Autowired MockMvc mvc;
    @Autowired JwtProvider jwt;
    @Autowired UniversityQueryService universities;
    @Autowired BookmarkService bookmarks;
    @Autowired MyInfoService myInfo;
    @Autowired AuthService auth;
    private Long userId;
    private Long schoolId;
    private String token;
    private String country;

    @BeforeEach
    void fixture() {
        String key = UUID.randomUUID().toString().replace("-", "");
        country = "Section-" + key;
        var user = User.builder().schoolEmail(key + "@hufs.ac.kr").nickname(key.substring(0, 12))
                .password("test-only").majorType(MajorType.PRIMARY).build();
        em.persist(user);
        userId = user.getId();
        token = "Bearer " + jwt.createAccessToken(userId);
        em.persist(UserProfile.builder().user(user).gpa(new BigDecimal("4.00")).build());
        var school = University.builder().name("Section School " + key).country(country).build();
        em.persist(school);
        schoolId = school.getId();
        var agreement = Agreement.builder().university(school).semester("9999-2")
                .isOpen(true).minGpa(new BigDecimal("3.0"))
                .announcedAt(LocalDate.of(2026, 7, 3)).importedAt(LocalDateTime.of(2026, 7, 3, 0, 0)).build();
        em.persist(agreement);
        em.persist(AgreementLanguageRequirement.builder().agreement(agreement).testType(LanguageTestType.IELTS)
                .minScore(new BigDecimal("6.5")).minSectionScore(new BigDecimal("6.0"))
                .conditionNote("6.5 (영역별 6.0이상)").build());
        em.persist(Bookmark.builder().user(user).university(school).build());
        em.flush();
        em.clear();
    }

    private void update(String sectionJson) throws Exception {
        mvc.perform(patch("/api/v1/users/me").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("""
                {"languageScores":[{"testType":"IELTS","score":6.5%s}]}
                """.formatted(sectionJson)))
                .andExpect(status().isOk());
        em.flush();
        em.clear();
    }

    @Test
    void httpUpdatePersistsSectionsAndAllThreeSchoolViewsAgree() throws Exception {
        update(",\"sectionScores\":{\"reading\":6.0,\"listening\":6.5,\"speaking\":7.0,\"writing\":6.0}");
        mvc.perform(get("/api/v1/users/me").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.languageScores[0].sectionScores.reading").value(6.0))
                .andExpect(jsonPath("$.data.languageScores[0].sectionScores.listening").value(6.5))
                .andExpect(jsonPath("$.data.languageScores[0].sectionScores.speaking").value(7.0))
                .andExpect(jsonPath("$.data.languageScores[0].sectionScores.writing").value(6.0));
        assertStatus("ELIGIBLE");
        mvc.perform(get("/api/v1/universities/" + schoolId).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.agreement.languageRequirements[0].minSectionScore").value(6.0))
                .andExpect(jsonPath("$.data.agreement.languageRequirements[0].conditionNote").value("6.5 (영역별 6.0이상)"));
        update(",\"sectionScores\":{\"reading\":5.5,\"listening\":6.5,\"speaking\":7.0,\"writing\":6.0}");
        assertStatus("NOT_ELIGIBLE");
        assertEquals(List.of("LANGUAGE_SECTION_BELOW_MIN"), universities.detail(userId, schoolId, null).eligibility().reasonCodes());
    }

    private void assertStatus(String status) {
        var detail = universities.detail(userId, schoolId, null).eligibility();
        assertEquals(status, detail.status());
        var listed = universities.list(userId, UniversitySearch.from(null, country, null, null, null));
        assertEquals(detail, listed.universities().getFirst().eligibility());
        var bookmarked = bookmarks.list(userId).universities().stream()
                .filter(s -> s.universityId().equals(schoolId)).findFirst().orElseThrow();
        assertEquals(detail, bookmarked.eligibility());
        assertEquals(0, listed.universities().getFirst().languageRequirements().getFirst().minSectionScore()
                .compareTo(new BigDecimal("6.0")));
        assertEquals(0, bookmarked.languageRequirements().getFirst().minSectionScore().compareTo(new BigDecimal("6.0")));
    }

    @Test
    void existingTotalOnlyScoreNeedsCheckAndOmittedLanguageScoresDoNotEraseSections() throws Exception {
        update("");
        assertStatus("NEEDS_CHECK");
        assertNull(myInfo.getMyInfo(userId).languageScores().getFirst().sectionScores());
        update(",\"sectionScores\":{\"reading\":6,\"listening\":6,\"speaking\":6,\"writing\":6}");
        mvc.perform(patch("/api/v1/users/me").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"gpa\":4.1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.languageScores[0].sectionScores.reading").value(6.0));
        em.flush();
        em.clear();
        assertStatus("ELIGIBLE");
        mvc.perform(patch("/api/v1/users/me").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"languageScores\":[]}"))
                .andExpect(status().isOk());
        assertTrue(myInfo.getMyInfo(userId).languageScores().isEmpty());
    }

    @Test
    void partialAndOutOfRangeSectionsReturn400WithoutReplacingSavedScores() throws Exception {
        update("");
        for (String sections : List.of("{\"reading\":6}",
                "{\"reading\":6.25,\"listening\":6,\"speaking\":6,\"writing\":6}",
                "{\"reading\":10,\"listening\":6,\"speaking\":6,\"writing\":6}")) {
            mvc.perform(patch("/api/v1/users/me").header("Authorization", token)
                    .contentType(MediaType.APPLICATION_JSON).content("""
                    {"languageScores":[{"testType":"IELTS","score":6.5,"sectionScores":%s}]}
                    """.formatted(sections)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("E400"))
                    .andExpect(jsonPath("$.errors").isNotEmpty());
        }
        assertNull(myInfo.getMyInfo(userId).languageScores().getFirst().sectionScores());
    }

    @Test
    void signupAlsoStoresSectionsFromTheSharedRequest() {
        String key = UUID.randomUUID().toString().replace("-", "");
        String email = key + "@hufs.ac.kr";
        var now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        em.persist(EmailVerification.builder().schoolEmail(email).codeHash("test-only")
                .expiresAt(now.plusMinutes(5)).verifiedAt(now).lastSentAt(now).build());
        var sections = new LanguageSectionScores(new BigDecimal("6"), new BigDecimal("6.5"),
                new BigDecimal("7"), new BigDecimal("6"));
        auth.signup(new SignupRequest(email, "test-only-password", key.substring(0, 12), MajorType.PRIMARY,
                new SignupRequest.ConsentsRequest(true, true), new BigDecimal("4"),
                List.of(new SignupRequest.LanguageScoreRequest(LanguageTestType.IELTS, new BigDecimal("6.5"), sections))));
        em.flush();
        em.clear();
        var saved = em.createQuery("select s from UserLanguageScore s where s.user.schoolEmail=:email", UserLanguageScore.class)
                .setParameter("email", email).getSingleResult();
        assertEquals(0, saved.getSectionScores().listening().compareTo(new BigDecimal("6.5")));
    }
}
