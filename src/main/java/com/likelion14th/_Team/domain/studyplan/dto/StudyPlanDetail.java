package com.likelion14th._Team.domain.studyplan.dto;

import java.math.BigDecimal;
import java.util.List;

public record StudyPlanDetail(
        Long studyPlanId,
        String name,
        Long universityId,
        String universityName,
        Calculation calculation,
        List<Item> items,
        BigDecimal totalExpectedCredit,
        BigDecimal semesterCreditLimit,
        boolean semesterCreditLimitWarning
) {
    public record Calculation(String status, String rule, String reasonCode) {
    }

    public record Item(
            Long itemId,
            String itemType,
            String courseName,
            BigDecimal foreignCredit,
            Long sourceCourseId,
            Long homeCourseId,
            String recognitionType,
            BigDecimal expectedCredit,
            boolean creditLimitWarning
    ) {
    }
}
