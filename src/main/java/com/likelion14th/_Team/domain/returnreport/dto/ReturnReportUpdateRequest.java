package com.likelion14th._Team.domain.returnreport.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 저장 API와 같은 형식에서 학교·학기만 뺀 전체 덮어쓰기 요청. 학교·학기를 보내도 무시한다 */
public record ReturnReportUpdateRequest(
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

        @Schema(description = "courseId가 있으면 수정, 없으면 추가, 보내지 않은 기존 행은 삭제")
        @NotNull(message = "수강 교과목 목록을 보내 주세요 (없으면 빈 배열)")
        List<@NotNull(message = "수강 교과목 항목이 비어 있습니다") @Valid ReturnReportCourseRequest> courses,

        @Schema(example = "기숙사 생활이 편리했고 수업 난이도는 보통이었다.")
        String reviewSummary
) implements ReturnReportContent {

    public ReturnReportUpdateRequest {
        housingType = TextValues.clean(housingType);
        insuranceProvider = TextValues.clean(insuranceProvider);
        reviewSummary = TextValues.clean(reviewSummary);
    }
}
