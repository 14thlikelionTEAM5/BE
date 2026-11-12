package com.likelion14th._Team.domain.coursecase.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.likelion14th._Team.global.enums.CourseCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** 인정 사례 저장·수정 요청. 수정도 같은 형식이며 바꾸지 않을 항목도 기존 값을 그대로 보낸다 */
public record CourseCaseRequest(
        @Schema(description = "인정 결과를 등록할 귀국보고서 수강 교과목 ID", example = "21")
        @NotNull(message = "대상 과목을 선택해 주세요")
        Long returnReportCourseId,

        @Schema(description = "MAJOR_REQUIRED(전필) / MAJOR_ELECTIVE(전선) / GENERAL(교양)",
                allowableValues = {"MAJOR_REQUIRED", "MAJOR_ELECTIVE", "GENERAL"}, example = "MAJOR_ELECTIVE")
        @NotNull(message = "인정 구분을 선택해 주세요")
        CourseCategory recognitionType,

        @Schema(description = "본교 인정 과목 ID. GENERAL이면 null로 보냅니다", example = "305", nullable = true)
        Long homeCourseId,

        @Schema(example = "3.0")
        @NotNull(message = "인정 학점을 입력해 주세요")
        @DecimalMin(value = "0.0", inclusive = false, message = "인정 학점은 0보다 커야 합니다")
        @DecimalMax(value = "99.9", message = "인정 학점은 99.9 이하여야 합니다")
        @Digits(integer = 2, fraction = 1, message = "인정 학점은 소수점 첫째 자리까지 입력할 수 있습니다")
        BigDecimal recognizedCredit
) {

    /** 전필·전선이면 본교 인정 과목이 필수. 교양은 보내도 무시한다 */
    @JsonIgnore
    @AssertTrue(message = "전공필수·전공선택은 본교 인정 과목을 선택해 주세요")
    public boolean isHomeCourseRequired() {
        if (recognitionType == null || recognitionType == CourseCategory.GENERAL) {
            return true; // 인정 구분은 @NotNull에서 처리
        }
        return homeCourseId != null;
    }

    /** 실제로 저장에 쓰는 본교 인정 과목 ID. 교양이면 항상 null */
    public Long resolvedHomeCourseId() {
        return recognitionType == CourseCategory.GENERAL ? null : homeCourseId;
    }
}