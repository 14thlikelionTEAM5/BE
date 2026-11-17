package com.likelion14th._Team.domain.returnreport.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/** 비용 항목. 보고서 표현 그대로의 문자열이며 통화 환산은 하지 않는다 */
public record ReturnReportCost(
        @Schema(example = "왕복 약 90만원")
        @Size(max = 200, message = "항공권 비용은 200자 이하여야 합니다")
        String flight,

        @Schema(example = "약 30만원")
        @Size(max = 200, message = "보험 비용은 200자 이하여야 합니다")
        String insurance,

        @Schema(example = "월 150만원")
        @Size(max = 200, message = "월 생활비는 200자 이하여야 합니다")
        String livingMonthly,

        @Schema(example = "약 1,200만원")
        @Size(max = 200, message = "총 비용은 200자 이하여야 합니다")
        String total
) {

    public static final ReturnReportCost EMPTY = new ReturnReportCost(null, null, null, null);

    public ReturnReportCost {
        flight = TextValues.clean(flight);
        insurance = TextValues.clean(insurance);
        livingMonthly = TextValues.clean(livingMonthly);
        total = TextValues.clean(total);
    }

    /** 요청에서 cost 자체가 null이면 하위 값 전부 null로 취급 */
    public static ReturnReportCost orEmpty(ReturnReportCost cost) {
        return cost == null ? EMPTY : cost;
    }
}
