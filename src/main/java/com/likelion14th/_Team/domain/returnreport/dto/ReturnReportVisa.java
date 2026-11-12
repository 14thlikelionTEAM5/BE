package com.likelion14th._Team.domain.returnreport.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record ReturnReportVisa(
        @Schema(example = "약 2주")
        @Size(max = 100, message = "비자 소요 기간은 100자 이하여야 합니다")
        String duration,

        @Schema(example = "Student Pass")
        @Size(max = 100, message = "비자 종류는 100자 이하여야 합니다")
        String type
) {

    public static final ReturnReportVisa EMPTY = new ReturnReportVisa(null, null);

    public ReturnReportVisa {
        duration = TextValues.clean(duration);
        type = TextValues.clean(type);
    }

    /** 요청에서 visa 자체가 null이면 하위 값 전부 null로 취급 */
    public static ReturnReportVisa orEmpty(ReturnReportVisa visa) {
        return visa == null ? EMPTY : visa;
    }
}
