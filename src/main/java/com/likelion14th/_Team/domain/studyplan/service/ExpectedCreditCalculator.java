package com.likelion14th._Team.domain.studyplan.service;

import com.likelion14th._Team.domain.course.entity.CreditPolicy;
import com.likelion14th._Team.domain.studyplan.dto.StudyPlanDetail.Calculation;
import com.likelion14th._Team.domain.studyplan.entity.StudyPlanItem;
import com.likelion14th._Team.domain.university.entity.University;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ExpectedCreditCalculator {

    public Calculation calculation(University university, CreditPolicy policy) {
        if (!Boolean.TRUE.equals(university.getIsAacsb()) && !Boolean.TRUE.equals(university.getIsEquis())) {
            return new Calculation("NOT_CALCULABLE", null, "ACCREDITATION_UNVERIFIED");
        }

        String rule = switch (university.getCreditSystem()) {
            case ECTS -> "ECTS_X0_6";
            case US_STYLE -> "AS_IS";
            case UK, OWN, UNKNOWN -> null;
        };
        if (rule == null) {
            return new Calculation("NOT_CALCULABLE", null, "CREDIT_SYSTEM_UNSUPPORTED");
        }
        if (policy == null) {
            return new Calculation("NOT_CALCULABLE", null, "CREDIT_POLICY_NOT_FOUND");
        }
        return new Calculation("CALCULABLE", rule, null);
    }

    public BigDecimal expectedCredit(StudyPlanItem item, Long homeCourseId, Calculation calculation,
                                     CreditPolicy policy) {
        if (calculation.rule() == null || item.getForeignCredit() == null) {
            return null;
        }
        boolean eligible = switch (item.getRecognitionType() == null ? "" : item.getRecognitionType()) {
            case "GENERAL" -> true;
            case "MAJOR_REQUIRED", "MAJOR_ELECTIVE" -> homeCourseId != null;
            default -> false;
        };
        if (!eligible) {
            return null;
        }
        return "ECTS_X0_6".equals(calculation.rule())
                ? item.getForeignCredit().multiply(policy.getEctsRate())
                : item.getForeignCredit();
    }

    public BigDecimal listTotal(University university, List<StudyPlanItem> items, CreditPolicy policy) {
        Calculation calculation = calculation(university, policy);
        if (calculation.rule() == null) {
            return null;
        }
        BigDecimal courseLimit = BigDecimal.valueOf(policy.getMaxCreditPerCourse());
        return items.stream()
                .filter(item -> item.getHomeCourse() != null)
                .map(item -> expectedCredit(item, item.getHomeCourse().getId(), calculation, policy))
                .filter(java.util.Objects::nonNull)
                .map(credit -> credit.min(courseLimit))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .min(BigDecimal.valueOf(policy.getMaxCreditPerSemester()));
    }
}
