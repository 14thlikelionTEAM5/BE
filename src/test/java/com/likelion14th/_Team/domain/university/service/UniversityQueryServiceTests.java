package com.likelion14th._Team.domain.university.service;

import com.likelion14th._Team.domain.university.dto.*;
import com.likelion14th._Team.domain.university.entity.*;
import com.likelion14th._Team.domain.university.repository.UniversityQueryRepository;
import com.likelion14th._Team.domain.university.repository.UniversityQueryRepository.SchoolRow;
import com.likelion14th._Team.global.enums.CreditSystem;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UniversityQueryServiceTests {
    private final UniversityQueryRepository repository = mock(UniversityQueryRepository.class);
    private final UniversityQueryService service = new UniversityQueryService(repository, new UniversityEligibilityCalculator(),
            new UniversityCardAssembler(repository, new UniversityEligibilityCalculator()));
    @BeforeEach void setup() {
        when(repository.profile(1L)).thenReturn(Optional.empty());
        when(repository.semesters()).thenReturn(List.of("2027-1"));
        when(repository.bookmarks(1L)).thenReturn(Set.of(1L));
        when(repository.schools(eq(1L),any(UniversitySearch.class),nullable(String.class))).thenReturn(List.of(
                new SchoolRow(school(1L,"Zulu","Belgium"),true,false),
                new SchoolRow(school(2L,"Alpha","USA"),false,true)));
    }
    private University school(Long id, String name, String country) {
        var u = University.builder().name(name).country(country).creditSystem(CreditSystem.ECTS).build();
        ReflectionTestUtils.setField(u,"id",id);
        return u;
    }
    @Test void includesSchoolsWithoutCurrentAnnouncementAndPreservesNulls() {
        var data = service.list(1L,UniversitySearch.from(null,null,null,null,null));
        assertEquals(2,data.totalCount());
        var first = data.universities().getFirst();
        assertEquals("Alpha",first.name());
        assertNull(first.agreementId());
        assertNull(first.languageRequirements());
        assertEquals(List.of("RECRUITMENT_STOPPED"), first.semesterChanges());
        assertTrue(first.hasExperienceData());
        assertFalse(first.isBookmarked());
        assertTrue(data.universities().get(1).isBookmarked());
        assertEquals("NOT_ELIGIBLE",first.eligibility().status());
    }
    @Test void delegatesSearchToDatabaseAndPreservesCountrySort() {
        var search=UniversitySearch.from("ALP","USA",null,null,null);
        when(repository.schools(1L,search,"2027-1")).thenReturn(List.of(new SchoolRow(school(2L,"Alpha","USA"),false,true)));
        var data=service.list(1L,search);
        assertEquals(1,data.totalCount());
        assertEquals("Alpha",data.universities().getFirst().name());
        verify(repository).schools(1L,search,"2027-1");
        assertEquals("Zulu",service.list(1L,UniversitySearch.from(null,null,null,null,"COUNTRY")).universities().getFirst().name());
    }
    @Test void eligibleAndLanguageFiltersExcludeUnknownSchools() {
        assertEquals(0,service.list(1L,UniversitySearch.from(null,null,null,"true",null)).totalCount());
        var languageSearch=UniversitySearch.from(null,null,"IELTS",null,null);
        when(repository.schools(1L,languageSearch,"2027-1")).thenReturn(List.of());
        assertEquals(0,service.list(1L,languageSearch).totalCount());
    }
    @Test void noAgreementsStillReturnsUniversitiesWithNullSemester() {
        when(repository.semesters()).thenReturn(List.of());
        var data = service.list(1L,UniversitySearch.from(null,null,null,null,null));
        assertNull(data.semester());
        assertEquals(2,data.totalCount());
    }
    @Test void countriesReturnEmptyArrayWhenNoUniversitiesExist() {
        when(repository.countries()).thenReturn(List.of());
        assertTrue(service.countries(1L).countries().isEmpty());
        verify(repository, never()).semesters();
        verify(repository, never()).agreementsForSchools(anyList(),anyList());
    }
    @Test void countriesReturnAggregatedCountsWithoutAgreementFilter() {
        var rows = List.of(new UniversityCountriesResponse.Country("네덜란드", 1L),
                new UniversityCountriesResponse.Country("독일", 2L));
        when(repository.countries()).thenReturn(rows);
        assertEquals(rows, service.countries(1L).countries());
        verify(repository, never()).semesters();
    }
    @Test void detailWithoutAnnouncementsPreservesSchoolAndIsNotEligible() {
        when(repository.university(1L)).thenReturn(Optional.of(school(1L,"Zulu","Belgium")));
        when(repository.studyPlanId(1L,1L)).thenReturn(Optional.empty());
        var data = service.detail(1L,1L,null);
        assertNull(data.semester());
        assertNull(data.agreement());
        assertTrue(data.availableSemesters().isEmpty());
        assertEquals("NOT_ELIGIBLE",data.eligibility().status());
        assertTrue(data.isBookmarked());
        assertFalse(data.hasStudyPlan());
        assertNull(data.studyPlanId());
    }
    @Test void detailDefaultsToSchoolLatestSemesterAndReturnsOwnedPlan() {
        when(repository.university(1L)).thenReturn(Optional.of(school(1L,"Zulu","Belgium")));
        var older = Agreement.builder().semester("2026-2").isOpen(true).build();
        var latest = Agreement.builder().semester("2027-1").isOpen(true).costSummary("official cost").build();
        ReflectionTestUtils.setField(latest,"id",10L);
        when(repository.schoolAgreements(1L)).thenReturn(List.of(latest,older));
        when(repository.studyPlanId(1L,1L)).thenReturn(Optional.of(7L));
        var data=service.detail(1L,1L,null);
        assertEquals("2027-1",data.semester());
        assertEquals(List.of("2027-1","2026-2"),data.availableSemesters());
        assertEquals("official cost",data.agreement().costSummary());
        assertTrue(data.agreement().languageRequirements().isEmpty());
        assertTrue(data.hasStudyPlan());
        assertEquals(7L,data.studyPlanId());
        verify(repository,never()).semesters();
    }
    @Test void explicitMissingSemesterDoesNotFallbackToOlderAnnouncement() {
        when(repository.university(1L)).thenReturn(Optional.of(school(1L,"Zulu","Belgium")));
        when(repository.schoolAgreements(1L)).thenReturn(List.of(Agreement.builder().semester("2027-1").build()));
        when(repository.studyPlanId(1L,1L)).thenReturn(Optional.empty());
        var data=service.detail(1L,1L,"2025-1");
        assertEquals("2025-1",data.semester());
        assertNull(data.agreement());
        assertEquals("NOT_ELIGIBLE",data.eligibility().status());
    }
    @Test void missingSchoolReturns404() {
        when(repository.university(999L)).thenReturn(Optional.empty());
        var error=assertThrows(com.likelion14th._Team.global.exception.CustomException.class,
                () -> service.detail(1L,999L,null));
        assertEquals("E404",error.getErrorCode().getCode());
    }
    @Test void closedAnnouncementHasSameEligibilityInListAndDetail() {
        var school = school(1L,"Zulu","Belgium");
        var closed = Agreement.builder().semester("2027-1").university(school).isOpen(false).build();
        ReflectionTestUtils.setField(closed,"id",10L);
        when(repository.university(1L)).thenReturn(Optional.of(school));
        when(repository.schoolAgreements(1L)).thenReturn(List.of(closed));
        when(repository.agreementsForSchools(anyList(),anyList())).thenReturn(List.of(closed));
        when(repository.schools(1L,UniversitySearch.from(null,"Belgium",null,null,null),"2027-1"))
                .thenReturn(List.of(new SchoolRow(school,true,false)));
        when(repository.studyPlanId(1L,1L)).thenReturn(Optional.empty());
        var detail = service.detail(1L,1L,"2027-1");
        var listed = service.list(1L,UniversitySearch.from(null,"Belgium",null,null,null)).universities().getFirst();
        assertEquals(listed.eligibility(),detail.eligibility());
        assertEquals(List.of("RECRUITMENT_STOPPED"),detail.eligibility().reasonCodes());
        assertEquals("이번 학기 모집 없음",detail.eligibility().message());
    }
}
