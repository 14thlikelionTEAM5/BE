package com.likelion14th._Team.domain.studyplan.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record StudyPlanItemUpdateResponse(
        Long itemId,
        String itemType,
        String courseName,
        BigDecimal foreignCredit,
        Long sourceCourseId,
        Long homeCourseId,
        String recognitionType,
        OffsetDateTime updatedAt
) {
}
