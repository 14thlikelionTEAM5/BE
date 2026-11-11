package com.likelion14th._Team.domain.university.service;

import com.likelion14th._Team.domain.university.dto.UniversitySearch;
import com.likelion14th._Team.domain.university.entity.*;
import com.likelion14th._Team.global.enums.LanguageTestType;
import com.likelion14th._Team.global.exception.CustomException;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UniversityQueryTests {
    private final UniversityEligibilityCalculator calculator = new UniversityEligibilityCalculator();
    private Agreement agreement(boolean open, Integer quota) {
        return Agreement.builder().isOpen(open).quota(quota).minGpa(new BigDecimal("3.0")).build();
    }
    private AgreementLanguageRequirement language(LanguageTestType test, String section) {
        return AgreementLanguageRequirement.builder().testType(test).minScore(new BigDecimal("6.5"))
                .minSectionScore(section == null ? null : new BigDecimal(section)).build();
    }
    @Test void missingAnnouncementIsNotRecruitmentStoppedBadge() {
        assertEquals(List.of("NO_CURRENT_AGREEMENT"), calculator.calculate(null, null, Map.of(), List.of()).reasonCodes());
        assertTrue(UniversityQueryService.changes(null, agreement(true, 2), true).isEmpty());
    }
    @Test void explicitClosureAndQuotaChangeProduceBothBadges() {
        assertEquals(List.of("QUOTA_CHANGED", "RECRUITMENT_STOPPED"),
                UniversityQueryService.changes(agreement(false, 0), agreement(true, 2), true));
        assertEquals("NOT_ELIGIBLE", calculator.calculate(agreement(false, 0), null, Map.of(), List.of()).status());
    }
    @Test void firstDatasetDoesNotMarkEverySchoolNew() {
        assertTrue(UniversityQueryService.changes(agreement(true, 2), null, false).isEmpty());
        assertEquals(List.of("NEW"), UniversityQueryService.changes(agreement(true, 2), null, true));
    }
    @Test void exactThresholdsPass() {
        assertEquals("ELIGIBLE", calculator.calculate(agreement(true, 2), new BigDecimal("3.00"),
                Map.of(LanguageTestType.IELTS, new BigDecimal("6.5")),
                List.of(language(LanguageTestType.IELTS, null))).status());
    }
    @Test void missingProfileNeedsCheckButKnownFailureWins() {
        assertEquals("NEEDS_CHECK", calculator.calculate(agreement(true, 2), null, Map.of(), List.of()).status());
        assertEquals("NOT_ELIGIBLE", calculator.calculate(agreement(true, 2), new BigDecimal("2.99"), Map.of(), List.of()).status());
    }
    @Test void sectionCriteriaCannotBeApprovedWithOnlyTotalScore() {
        assertEquals("NEEDS_CHECK", calculator.calculate(agreement(true, 2), new BigDecimal("4.0"),
                Map.of(LanguageTestType.IELTS, new BigDecimal("7.0")),
                List.of(language(LanguageTestType.IELTS, "6.0"))).status());
    }
    @Test void alternativePassingTestWinsOverFailedTest() {
        assertEquals("ELIGIBLE", calculator.calculate(agreement(true, 2), new BigDecimal("4.0"),
                Map.of(LanguageTestType.IELTS, new BigDecimal("5.0"), LanguageTestType.TOEFL_NEW, new BigDecimal("7.0")),
                List.of(language(LanguageTestType.IELTS, null), language(LanguageTestType.TOEFL_NEW, null))).status());
    }
    @Test void knownLanguageFailureIsReported() {
        assertEquals(List.of("LANGUAGE_BELOW_MIN"), calculator.calculate(agreement(true, 2), new BigDecimal("4.0"),
                Map.of(LanguageTestType.IELTS, new BigDecimal("6.0")),
                List.of(language(LanguageTestType.IELTS, null))).reasonCodes());
    }
    @Test void invalidFiltersAreRejected() {
        assertThrows(CustomException.class, () -> UniversitySearch.from(null,null,"TOEFL",null,null));
        assertThrows(CustomException.class, () -> UniversitySearch.from(null,null,null,"yes",null));
        assertThrows(CustomException.class, () -> UniversitySearch.from(null,null,null,null,"RECENT"));
        assertEquals("NAME", UniversitySearch.from(null,null,null,null,null).sort());
    }
}
