package com.likelion14th._Team.domain.studyplan.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record StudyPlanSummary(
        Long studyPlanId,
        Long universityId,
        String universityName,
        String name,
        int courseCount,
        BigDecimal totalExpectedCredits,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
