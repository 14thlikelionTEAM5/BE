package com.likelion14th._Team.domain.university.service;

import static org.junit.jupiter.api.Assertions.*;

import com.likelion14th._Team.domain.university.entity.Agreement;
import com.likelion14th._Team.domain.university.entity.AgreementLanguageRequirement;
import com.likelion14th._Team.domain.university.service.UniversityEligibilityCalculator.LanguageScore;
import com.likelion14th._Team.domain.user.entity.LanguageSectionScores;
import com.likelion14th._Team.global.enums.LanguageTestType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LanguageSectionEligibilityTests {
    private final UniversityEligibilityCalculator calculator = new UniversityEligibilityCalculator();
    private final Agreement agreement = Agreement.builder().isOpen(true).minGpa(new BigDecimal("3.0")).build();

    private AgreementLanguageRequirement requirement(LanguageTestType type, String total, String section, String note) {
        return AgreementLanguageRequirement.builder().testType(type).minScore(new BigDecimal(total))
                .minSectionScore(section == null ? null : new BigDecimal(section)).conditionNote(note).build();
    }

    private LanguageScore score(String total, String reading, String others) {
        return new LanguageScore(new BigDecimal(total), reading == null ? null : new LanguageSectionScores(
                new BigDecimal(reading), new BigDecimal(others), new BigDecimal(others), new BigDecimal(others)));
    }

    @ParameterizedTest
    @CsvSource({"IELTS,6.5,6.0,5.5", "TOEFL_IBT,90,21,20", "TOEFL_NEW,4.5,4.0,3.5"})
    void checksThresholdsAndExplainsMissingOrFailedSections(LanguageTestType type, String total, String min, String low) {
        var requirement = requirement(type, total, min, total + " (영역별 " + min + "이상)");
        var matched = calculator.calculate(agreement, new BigDecimal("4.0"),
                Map.of(type, score(total, min, min)), List.of(requirement));
        assertEquals("ELIGIBLE", matched.status());
        var below = calculator.calculate(agreement, new BigDecimal("4.0"),
                Map.of(type, score(total, low, min)), List.of(requirement));
        assertEquals("NOT_ELIGIBLE", below.status());
        assertEquals(List.of("LANGUAGE_SECTION_BELOW_MIN"), below.reasonCodes());
        var missing = calculator.calculate(agreement, new BigDecimal("4.0"),
                Map.of(type, score(total, null, null)), List.of(requirement));
        assertEquals("NEEDS_CHECK", missing.status());
        assertEquals(List.of("LANGUAGE_SECTION_SCORE_NOT_PROVIDED"), missing.reasonCodes());
        assertTrue(missing.message().contains("영역별 점수"));
    }

    @Test
    void unrelatedOrInconsistentConditionNoteStillRequiresReview() {
        for (String note : List.of("6.5 (영역별 6.0이상), 면접 필수", "R:7 W:7", "6.5 (영역별 7.0이상)")) {
            var actual = calculator.calculate(agreement, new BigDecimal("4.0"),
                    Map.of(LanguageTestType.IELTS, score("6.5", "6.0", "6.0")),
                    List.of(requirement(LanguageTestType.IELTS, "6.5", "6.0", note)));
            assertEquals(List.of("LANGUAGE_DETAIL_NEEDS_CHECK"), actual.reasonCodes());
        }
    }

    @Test
    void oneCompletelyPassingTestWinsOverFailedSectionsOrUnknownAlternative() {
        var requirements = List.of(requirement(LanguageTestType.IELTS, "6.5", "6.0", null),
                requirement(LanguageTestType.TOEFL_IBT, "90", null, "90"));
        assertEquals("ELIGIBLE", calculator.calculate(agreement, new BigDecimal("4.0"),
                Map.of(LanguageTestType.IELTS, score("6.5", "5.5", "6.0"),
                        LanguageTestType.TOEFL_IBT, score("90", null, null)), requirements).status());
    }

    @Test
    void unconfirmedAlternativeIsNotRejectedBecauseAnotherTestFails() {
        var actual = calculator.calculate(agreement, new BigDecimal("4.0"),
                Map.of(LanguageTestType.IELTS, score("6.5", null, null),
                        LanguageTestType.TOEFL_IBT, score("80", null, null)),
                List.of(requirement(LanguageTestType.IELTS, "6.5", "6.0", null),
                        requirement(LanguageTestType.TOEFL_IBT, "90", null, null)));
        assertEquals("NEEDS_CHECK", actual.status());
    }

    @Test
    void sectionScoresDoNotCompensateForInsufficientTotalAndAreNotRequiredWithoutSectionCriteria() {
        var requirement = requirement(LanguageTestType.IELTS, "6.5", "6.0", null);
        assertEquals(List.of("LANGUAGE_BELOW_MIN"), calculator.calculate(agreement, new BigDecimal("4"),
                Map.of(LanguageTestType.IELTS, score("6.0", "7.0", "7.0")), List.of(requirement)).reasonCodes());
        assertEquals("ELIGIBLE", calculator.calculate(agreement, new BigDecimal("4"),
                Map.of(LanguageTestType.IELTS, score("6.5", null, null)),
                List.of(requirement(LanguageTestType.IELTS, "6.5", null, "6.5"))).status());
    }
}
