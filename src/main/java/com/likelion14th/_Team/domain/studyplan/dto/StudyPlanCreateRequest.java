package com.likelion14th._Team.domain.studyplan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record StudyPlanCreateRequest(
        @NotNull @Positive Long universityId,
        @NotBlank @Size(max = 100) String name
) {
}
