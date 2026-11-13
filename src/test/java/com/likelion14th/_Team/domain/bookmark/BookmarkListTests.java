package com.likelion14th._Team.domain.bookmark;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.likelion14th._Team.domain.bookmark.entity.Bookmark;
import com.likelion14th._Team.domain.bookmark.service.BookmarkService;
import com.likelion14th._Team.domain.university.entity.Agreement;
import com.likelion14th._Team.domain.university.entity.AgreementLanguageRequirement;
import com.likelion14th._Team.domain.university.entity.University;
import com.likelion14th._Team.domain.university.service.UniversityQueryService;
import com.likelion14th._Team.domain.user.entity.User;
import com.likelion14th._Team.domain.user.entity.UserLanguageScore;
import com.likelion14th._Team.domain.user.entity.UserProfile;
import com.likelion14th._Team.global.enums.LanguageTestType;
import com.likelion14th._Team.global.enums.MajorType;
import com.likelion14th._Team.global.security.JwtProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.Filter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.hibernate.SessionFactory;
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

@ActiveProfiles("test")
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Transactional
class BookmarkListTests {
    @Autowired EntityManager em;
    @Autowired EntityManagerFactory emf;
    @Autowired BookmarkService service;
    @Autowired UniversityQueryService universities;
    @Autowired JwtProvider jwt;
    @Autowired WebApplicationContext context;
    @Autowired @Qualifier("springSecurityFilterChain") Filter security;
    private MockMvc mvc;
    private User viewer;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
        viewer = user();
        // 정상 가입 사용자는 GPA를 필수로 입력한다.
        em.persist(UserProfile.builder().user(viewer).gpa(new BigDecimal("4.0")).build());
    }

    private User user() {
        String key = UUID.randomUUID().toString().replace("-", "");
        User user = User.builder().schoolEmail(key + "@example.hufs.ac.kr")
                .nickname(key.substring(0, 12)).majorType(MajorType.MINOR)
                .password(UUID.randomUUID().toString()).build();
        em.persist(user);
        return user;
    }

    private University school() {
        University school = University.builder().name("bookmark-list-" + UUID.randomUUID()).country("테스트").build();
        em.persist(school);
        return school;
    }

    private Bookmark save(User user, University school) {
        Bookmark bookmark = Bookmark.builder().user(user).university(school).build();
        em.persist(bookmark);
        return bookmark;
    }

    private Agreement agreement(University school, String semester, BigDecimal minGpa, boolean open) {
        Agreement agreement = Agreement.builder().university(school).semester(semester).minGpa(minGpa)
                .quota(2).isOpen(open).isBusinessOnly(true)
                .announcedAt(LocalDate.now()).importedAt(LocalDateTime.now()).build();
        em.persist(agreement);
        return agreement;
    }

    private void requirement(Agreement agreement, LanguageTestType type, String score) {
        em.persist(AgreementLanguageRequirement.builder().agreement(agreement).testType(type)
                .minScore(new BigDecimal(score)).build());
    }

    @Test
    void latestSemesterIsGlobalAndOrderIsCreatedAtThenIdWithoutLeakingOtherUsers() {
        University oldOnly = school(), current = school(), noAgreement = school();
        agreement(school(), "9999-2", BigDecimal.ONE, true);
        agreement(oldOnly, "9999-1", BigDecimal.ONE, true);
        agreement(current, "9999-2", new BigDecimal("3.0"), true);
        Bookmark first = save(viewer, oldOnly), second = save(viewer, current), third = save(viewer, noAgreement);
        save(user(), school());
        em.flush();
        for (Bookmark bookmark : List.of(first, second)) {
            em.createNativeQuery("update bookmark set created_at='2026-01-01 00:00:00' where id=:id")
                    .setParameter("id", bookmark.getId()).executeUpdate();
        }
        em.createNativeQuery("update bookmark set created_at='2026-02-01 00:00:00' where id=:id")
                .setParameter("id", third.getId()).executeUpdate();
        em.clear();
        var response = service.list(viewer.getId());
        assertEquals("9999-2", response.semester());
        assertEquals(3, response.totalCount());
        assertEquals(List.of(noAgreement.getId(), current.getId(), oldOnly.getId()),
                response.universities().stream().map(s -> s.universityId()).toList());
        assertTrue(response.universities().stream().allMatch(s -> s.isBookmarked()));
        assertNull(response.universities().getFirst().agreementId());
        assertNull(response.universities().getFirst().languageRequirements());
        assertEquals("NOT_ELIGIBLE", response.universities().getFirst().eligibility().status());
        assertEquals(List.of("NO_CURRENT_AGREEMENT"), response.universities().getFirst().eligibility().reasonCodes());
        assertTrue(response.universities().get(1).languageRequirements().isEmpty());
        assertNull(response.universities().get(2).agreementId());
    }

    @Test
    void eligibilityAndLanguageOrderMatchUniversityDetailWithoutFilteringFailures() {
        em.persist(UserLanguageScore.builder().user(viewer).testType(LanguageTestType.IELTS)
                .score(new BigDecimal("7.0")).build());
        University eligible = school(), highGpa = school(), stopped = school();
        Agreement first = agreement(eligible, "9999-2", new BigDecimal("3.0"), true);
        requirement(first, LanguageTestType.IELTS, "6.5");
        requirement(first, LanguageTestType.TOEFL_IBT, "90");
        agreement(highGpa, "9999-2", new BigDecimal("4.5"), true);
        agreement(stopped, "9999-2", new BigDecimal("3.0"), false);
        save(viewer, eligible); save(viewer, highGpa); save(viewer, stopped);
        em.flush(); em.clear();
        var response = service.list(viewer.getId());
        assertEquals(3, response.totalCount());
        for (var card : response.universities()) {
            assertEquals(universities.detail(viewer.getId(), card.universityId(), response.semester()).eligibility(),
                    card.eligibility());
        }
        var card = response.universities().stream().filter(s -> s.universityId().equals(eligible.getId())).findFirst().orElseThrow();
        assertEquals("ELIGIBLE", card.eligibility().status());
        assertEquals(List.of("TOEFL_IBT", "IELTS"), card.languageRequirements().stream().map(r -> r.testType()).toList());
        assertEquals(2, response.universities().stream().filter(s -> s.eligibility().status().equals("NOT_ELIGIBLE")).count());
    }

    @Test
    void httpReturnsEmptyAndCardShapeAndRequiresAuthentication() throws Exception {
        String token = "Bearer " + jwt.createAccessToken(viewer.getId());
        mvc.perform(get("/api/v1/bookmarks").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalCount").value(0))
                .andExpect(jsonPath("$.data.universities").isEmpty());
        save(viewer, school());
        mvc.perform(get("/api/v1/bookmarks").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.message").value("찜 목록 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.universities[0].isBookmarked").value(true))
                .andExpect(jsonPath("$.data.universities[0].creditSystem").doesNotExist())
                .andExpect(jsonPath("$.data.universities[0].isAacsb").doesNotExist());
        mvc.perform(get("/api/v1/bookmarks")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
    }

    @Test
    void readsRemainBoundedWithManyBookmarksAndRequirements() {
        for (int i = 0; i < 30; i++) {
            University school = school();
            requirement(agreement(school, "9999-2", BigDecimal.ONE, true), LanguageTestType.IELTS, "6.5");
            save(viewer, school);
        }
        em.flush(); em.clear();
        var statistics = emf.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        assertEquals(30, service.list(viewer.getId()).totalCount());
        assertTrue(statistics.getPrepareStatementCount() <= 5);
    }
    @Test
    void abnormalMissingGpaIsHandledDefensivelyAndMatchesUniversityApis() {
        // 정상 가입 흐름과 별개로, 기존/비정상 데이터의 GPA 누락을 방어한다.
        em.flush();
        em.createQuery("delete from UserProfile p where p.user.id = :id")
                .setParameter("id", viewer.getId()).executeUpdate();
        University school = school();
        Agreement agreement = agreement(school, "9999-2", new BigDecimal("3.0"), true);
        requirement(agreement, LanguageTestType.IELTS, "7.0");
        em.persist(UserLanguageScore.builder().user(viewer).testType(LanguageTestType.IELTS)
                .score(new BigDecimal("4.0")).build());
        save(viewer, school);
        var result = service.list(viewer.getId()).universities().getFirst().eligibility();
        assertEquals("NEEDS_CHECK", result.status());
        assertEquals(List.of("GPA_NOT_PROVIDED"), result.reasonCodes());
        assertEquals(result, universities.detail(viewer.getId(), school.getId(), "9999-2").eligibility());
        var fromList = universities.list(viewer.getId(),
                com.likelion14th._Team.domain.university.dto.UniversitySearch.from(school.getName(), null, null, null, null));
        assertEquals(result, fromList.universities().getFirst().eligibility());
    }

    @Test
    void unknownSchoolGpaAndLowUserGpaDoNotFallThroughToLanguage() {
        em.createQuery("update UserProfile p set p.gpa = :gpa where p.user.id = :id")
                .setParameter("gpa", new BigDecimal("2.0")).setParameter("id", viewer.getId()).executeUpdate();
        em.persist(UserLanguageScore.builder().user(viewer).testType(LanguageTestType.IELTS)
                .score(new BigDecimal("4.0")).build());
        University unknown = school(), low = school();
        requirement(agreement(unknown, "9999-2", null, true), LanguageTestType.IELTS, "7.0");
        requirement(agreement(low, "9999-2", new BigDecimal("3.0"), true), LanguageTestType.IELTS, "7.0");
        save(viewer, unknown); save(viewer, low);
        for (var card : service.list(viewer.getId()).universities()) {
            if (card.universityId().equals(unknown.getId())) {
                assertEquals("NEEDS_CHECK", card.eligibility().status());
                assertEquals(List.of("GPA_REQUIREMENT_UNKNOWN"), card.eligibility().reasonCodes());
            } else {
                assertEquals("NOT_ELIGIBLE", card.eligibility().status());
                assertEquals(List.of("GPA_BELOW_MIN"), card.eligibility().reasonCodes());
            }
        }
    }

}
