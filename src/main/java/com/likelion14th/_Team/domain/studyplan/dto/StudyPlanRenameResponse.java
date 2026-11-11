package com.likelion14th._Team.domain.studyplan.dto;

import java.time.OffsetDateTime;

public record StudyPlanRenameResponse(Long studyPlanId, String name, OffsetDateTime updatedAt) {
}
