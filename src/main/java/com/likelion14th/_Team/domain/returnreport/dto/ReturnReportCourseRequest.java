package com.likelion14th._Team.domain.returnreport.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** 수강 교과목 한 행. courseId는 수정 API에서만 사용한다 (있으면 수정, 없으면 추가) */
public record ReturnReportCourseRequest(
        @Schema(description = "수정 API에서 기존 행을 수정할 때만 보냅니다", example = "21")
        Long courseId,

        @Schema(example = "MKT2101")
        @Size(max = 50, message = "교과목 코드는 50자 이하여야 합니다")
        String courseCode,

        @Schema(example = "Principles of Marketing")
        @NotBlank(message = "영문 교과목명을 입력해 주세요")
        @Size(max = 200, message = "영문 교과목명은 200자 이하여야 합니다")
        String courseNameEn,

        @Schema(example = "4.0")
        @NotNull(message = "외국 학점을 입력해 주세요")
        @DecimalMin(value = "0.0", inclusive = false, message = "외국 학점은 0보다 커야 합니다")
        @DecimalMax(value = "99.9", message = "외국 학점은 99.9 이하여야 합니다")
        @Digits(integer = 2, fraction = 1, message = "외국 학점은 소수점 첫째 자리까지 입력할 수 있습니다")
        BigDecimal foreignCredit,

        /* Integer로 받으면 4.5가 4로 잘려 통과하므로 BigDecimal로 받아 정수인지 검증한다 (4.0도 정수 표기가 아니라 400) */
        @Schema(description = "수업 만족도 1~5 (정수)", type = "integer", example = "4")
        @Min(value = 1, message = "수업 만족도는 1~5 사이여야 합니다")
        @Max(value = 5, message = "수업 만족도는 1~5 사이여야 합니다")
        @Digits(integer = 1, fraction = 0, message = "수업 만족도는 정수여야 합니다")
        BigDecimal satisfaction
) {

    public ReturnReportCourseRequest {
        courseCode = TextValues.clean(courseCode);
        courseNameEn = TextValues.clean(courseNameEn);
    }

    /** 검증을 통과한 값(정수 1~5)만 들어오므로 정확히 변환된다 */
    public Integer satisfactionValue() {
        return satisfaction == null ? null : satisfaction.intValueExact();
    }
}
