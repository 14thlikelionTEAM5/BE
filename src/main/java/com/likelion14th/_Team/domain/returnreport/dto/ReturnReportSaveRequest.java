package com.likelion14th._Team.domain.returnreport.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ReturnReportSaveRequest(
        @Schema(example = "12")
        @NotNull(message = "학교를 선택해 주세요")
        Long universityId,

        @Schema(description = "YYYY-1 또는 YYYY-2", example = "2026-2")
        @NotBlank(message = "학기를 입력해 주세요")
        @Pattern(regexp = "^\\d{4}-[12]$", message = "학기는 YYYY-1 또는 YYYY-2 형식이어야 합니다")
        String semester,

        @Valid
        ReturnReportCost cost,

        @Schema(description = "DORM(기숙사) / SELF(자취) / OTHER(기타)", allowableValues = {"DORM", "SELF", "OTHER"})
        @Pattern(regexp = "DORM|SELF|OTHER", message = "숙소 유형은 DORM, SELF, OTHER 중 하나여야 합니다")
        String housingType,

        @Valid
        ReturnReportVisa visa,

        @Schema(example = "삼성화재")
        @Size(max = 200, message = "보험 구입처는 200자 이하여야 합니다")
        String insuranceProvider,

        @Schema(description = "수강 교과목. 없으면 빈 배열")
        @NotNull(message = "수강 교과목 목록을 보내 주세요 (없으면 빈 배열)")
        List<@NotNull(message = "수강 교과목 항목이 비어 있습니다") @Valid ReturnReportCourseRequest> courses,

        @Schema(example = "기숙사 생활이 편리했고 수업 난이도는 보통이었다.")
        String reviewSummary
) implements ReturnReportContent {

    public ReturnReportSaveRequest {
        semester = TextValues.clean(semester);
        housingType = TextValues.clean(housingType);
        insuranceProvider = TextValues.clean(insuranceProvider);
        reviewSummary = TextValues.clean(reviewSummary);
    }
}
