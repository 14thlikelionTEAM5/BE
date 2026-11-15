package com.likelion14th._Team.domain.university;

import com.likelion14th._Team.domain.bookmark.dto.BookmarkListResponse;
import com.likelion14th._Team.domain.bookmark.entity.Bookmark;
import com.likelion14th._Team.domain.bookmark.service.BookmarkService;
import com.likelion14th._Team.domain.university.dto.UniversityListResponse;
import com.likelion14th._Team.domain.university.dto.UniversitySearch;
import com.likelion14th._Team.domain.university.entity.Agreement;
import com.likelion14th._Team.domain.university.entity.AgreementLanguageRequirement;
import com.likelion14th._Team.domain.university.entity.University;
import com.likelion14th._Team.domain.university.service.UniversityQueryService;
import com.likelion14th._Team.domain.user.entity.User;
import com.likelion14th._Team.domain.user.entity.UserLanguageScore;
import com.likelion14th._Team.domain.user.entity.UserProfile;
import com.likelion14th._Team.global.enums.LanguageTestType;
import com.likelion14th._Team.global.enums.MajorType;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class UniversityBookmarkAssemblyTests {
    @Autowired EntityManager em;
    @Autowired UniversityQueryService universities;
    @Autowired BookmarkService bookmarks;
    private Long userId;
    private String country;
    private final List<Long> ids = new ArrayList<>();

    @BeforeEach
    void fixture() {
        String key = UUID.randomUUID().toString().replace("-", "");
        country = "Assembly-" + key;
        var user = User.builder().schoolEmail(key + "@example.hufs.ac.kr").nickname(key.substring(0, 12))
                .password("test-only").majorType(MajorType.PRIMARY).build();
        em.persist(user);
        userId = user.getId();
        em.persist(UserProfile.builder().user(user).gpa(new BigDecimal("4.00")).build());
        em.persist(UserLanguageScore.builder().user(user).testType(LanguageTestType.IELTS)
                .score(new BigDecimal("7.0")).build());
        String[] names = {"Zulu", "Alpha", "Beta", "Delta", "Gamma"};
        for (int i = 0; i < names.length; i++) {
            var school = University.builder().name(key + "-" + names[i]).country(country).build();
            em.persist(school);
            ids.add(school.getId());
            var agreement = agreement(school, i == 2 ? "9999-1" : "9999-2", i != 1);
            if (i == 0) {
                requirement(agreement, LanguageTestType.IELTS, "6.5");
                requirement(agreement, LanguageTestType.TOEFL_IBT, "90");
            } else if (i == 3) {
                requirement(agreement, LanguageTestType.TOEFL_NEW, "4.5");
            }
            var bookmark = Bookmark.builder().user(user).university(school).build();
            em.persist(bookmark);
            em.flush();
            // 등록일이 같으면 ID 내림차순이 되는지 확인한다.
            em.createNativeQuery("update bookmark set created_at='2026-01-01 00:00:00' where id=:id")
                    .setParameter("id", bookmark.getId()).executeUpdate();
        }
        em.flush();
        em.clear();
    }

    private Agreement agreement(University school, String semester, boolean open) {
        var agreement = Agreement.builder().university(school).semester(semester).isOpen(open).quota(2)
                .minGpa(new BigDecimal("3.00")).announcedAt(LocalDate.of(2026, 7, 3))
                .importedAt(LocalDateTime.of(2026, 7, 3, 0, 0)).build();
        em.persist(agreement);
        return agreement;
    }

    private void requirement(Agreement agreement, LanguageTestType type, String score) {
        em.persist(AgreementLanguageRequirement.builder().agreement(agreement).testType(type)
                .minScore(new BigDecimal(score)).build());
    }

    private UniversityListResponse list(String eligibleOnly) {
        return universities.list(userId, UniversitySearch.from(null, country, null, eligibleOnly, null));
    }

    private void assertCommon(UniversityListResponse listed, BookmarkListResponse saved) {
        assertEquals(listed.semester(), saved.semester());
        var byId = listed.universities().stream().collect(Collectors.toMap(
                UniversityListResponse.School::universityId, Function.identity()));
        for (var bookmark : saved.universities()) {
            var school = byId.get(bookmark.universityId());
            assertEquals(school.name(), bookmark.name());
            assertEquals(school.country(), bookmark.country());
            assertEquals(school.agreementId(), bookmark.agreementId());
            assertEquals(school.quota(), bookmark.quota());
            assertEquals(school.minGpa(), bookmark.minGpa());
            assertEquals(school.languageRequirements(), bookmark.languageRequirements());
            assertEquals(school.eligibility(), bookmark.eligibility());
            assertTrue(bookmark.isBookmarked());
            assertTrue(school.isBookmarked());
        }
    }

    @Test
    void sameInputsProduceSameCardsAndKeepEachListOrderingAndBadges() {
        var listed = list(null);
        var saved = bookmarks.list(userId);
        assertCommon(listed, saved);
        assertEquals(5, listed.totalCount());
        assertEquals(List.of(ids.get(1), ids.get(2), ids.get(3), ids.get(4), ids.get(0)),
                listed.universities().stream().map(UniversityListResponse.School::universityId).toList());
        assertEquals(List.of(ids.get(4), ids.get(3), ids.get(2), ids.get(1), ids.get(0)),
                saved.universities().stream().map(BookmarkListResponse.School::universityId).toList());
        var byId = listed.universities().stream().collect(Collectors.toMap(
                UniversityListResponse.School::universityId, Function.identity()));
        assertEquals("ELIGIBLE", byId.get(ids.get(0)).eligibility().status());
        assertEquals(List.of("TOEFL_IBT", "IELTS"), byId.get(ids.get(0)).languageRequirements().stream()
                .map(UniversityListResponse.LanguageRequirement::testType).toList());
        assertEquals(List.of("RECRUITMENT_STOPPED"), byId.get(ids.get(1)).eligibility().reasonCodes());
        assertTrue(byId.get(ids.get(1)).semesterChanges().contains("RECRUITMENT_STOPPED"));
        assertEquals(List.of("NO_CURRENT_AGREEMENT"), byId.get(ids.get(2)).eligibility().reasonCodes());
        assertNull(byId.get(ids.get(2)).languageRequirements());
        assertEquals(List.of("LANGUAGE_SCORE_NOT_PROVIDED"), byId.get(ids.get(3)).eligibility().reasonCodes());
        assertEquals(List.of("LANGUAGE_REQUIREMENT_UNKNOWN"), byId.get(ids.get(4)).eligibility().reasonCodes());
        assertTrue(byId.get(ids.get(4)).languageRequirements().isEmpty());
        assertEquals(List.of(ids.get(0)), list("true").universities().stream()
                .map(UniversityListResponse.School::universityId).toList());
        assertEquals(5, bookmarks.list(userId).totalCount());
    }

    @Test
    void optionalLanguageScoresAndGpaFailureStayConsistent() {
        em.createQuery("delete from UserLanguageScore s where s.user.id=:id")
                .setParameter("id", userId).executeUpdate();
        em.clear();
        var missing = list(null);
        assertCommon(missing, bookmarks.list(userId));
        var card = missing.universities().stream().filter(s -> s.universityId().equals(ids.get(0))).findFirst().orElseThrow();
        assertEquals("NEEDS_CHECK", card.eligibility().status());
        assertEquals("어학 성적이 없어 지원 자격을 확인해야 해요.", card.eligibility().message());
        em.createQuery("update UserProfile p set p.gpa=2.0 where p.user.id=:id")
                .setParameter("id", userId).executeUpdate();
        em.clear();
        var lowGpa = list(null);
        assertCommon(lowGpa, bookmarks.list(userId));
        assertEquals(List.of("GPA_BELOW_MIN"), lowGpa.universities().stream()
                .filter(s -> s.universityId().equals(ids.get(0))).findFirst().orElseThrow().eligibility().reasonCodes());
    }

    @Test
    void emptyBookmarksKeepLatestSemesterAndDoNotChangeUniversityList() {
        em.createQuery("delete from Bookmark b where b.user.id=:id").setParameter("id", userId).executeUpdate();
        em.clear();
        var saved = bookmarks.list(userId);
        assertEquals("9999-2", saved.semester());
        assertEquals(0, saved.totalCount());
        assertTrue(saved.universities().isEmpty());
        assertEquals(5, list(null).totalCount());
    }
}
