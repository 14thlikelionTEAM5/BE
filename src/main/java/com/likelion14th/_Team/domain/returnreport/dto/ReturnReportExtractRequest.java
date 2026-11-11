package com.likelion14th._Team.domain.returnreport.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 붙여넣은 귀국보고서 원문. 저장·로깅하지 않는다 (검증 사유에도 입력값을 넣지 않음) */
public record ReturnReportExtractRequest(
        @Schema(description = "귀국보고서 텍스트 (최대 20,000자)", example = "저는 2026년 가을학기에 싱가포르 국립대학교에서 교환학생으로...")
        @NotBlank(message = "귀국보고서 내용을 입력해 주세요")
        @Size(max = 20000, message = "귀국보고서는 20,000자 이하여야 합니다")
        String text
) {

    /** 원문이 로그에 찍히지 않도록 toString에서 내용을 숨긴다 */
    @Override
    public String toString() {
        return "ReturnReportExtractRequest[text=(" + (text == null ? 0 : text.length()) + " chars)]";
    }
}
