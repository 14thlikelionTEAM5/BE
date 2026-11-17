package com.likelion14th._Team.domain.studyplan.service;

import com.likelion14th._Team.domain.course.entity.CreditPolicy;
import com.likelion14th._Team.domain.studyplan.entity.StudyPlanItem;
import com.likelion14th._Team.domain.university.entity.University;
import com.likelion14th._Team.global.enums.CreditSystem;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExpectedCreditCalculatorTests {

    private final ExpectedCreditCalculator calculator = new ExpectedCreditCalculator();
    private final University university = University.builder()
            .isAacsb(true).creditSystem(CreditSystem.ECTS).build();
    private final CreditPolicy policy = CreditPolicy.builder().build();

    @Test
    void generalCourseWithoutHomeCourseContributesToListTotal() {
        StudyPlanItem item = mock(StudyPlanItem.class);
        when(item.getRecognitionType()).thenReturn("GENERAL");
        when(item.getForeignCredit()).thenReturn(new BigDecimal("5.0"));

        assertEquals(0, new BigDecimal("3").compareTo(
                calculator.listTotal(university, List.of(item), policy)));
    }

    @Test
    void unassignedMajorCourseStillContributesZero() {
        StudyPlanItem item = mock(StudyPlanItem.class);
        when(item.getRecognitionType()).thenReturn("MAJOR_ELECTIVE");
        when(item.getForeignCredit()).thenReturn(new BigDecimal("5.0"));

        assertEquals(0, BigDecimal.ZERO.compareTo(
                calculator.listTotal(university, List.of(item), policy)));
    }

    @Test
    void generalCoursesStillRespectCourseAndSemesterLimits() {
        StudyPlanItem item = mock(StudyPlanItem.class);
        when(item.getRecognitionType()).thenReturn("GENERAL");
        when(item.getForeignCredit()).thenReturn(new BigDecimal("10.0"));

        assertEquals(0, new BigDecimal("18").compareTo(
                calculator.listTotal(university, List.of(item, item, item, item, item, item, item), policy)));
    }
}
