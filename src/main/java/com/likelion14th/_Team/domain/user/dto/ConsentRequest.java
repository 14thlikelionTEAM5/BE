package com.likelion14th._Team.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** consentType은 잘못된 값일 때 필드별 사유를 내려주기 위해 문자열로 받아 서비스에서 검증한다 */
public record ConsentRequest(
        @Schema(description = "동의 유형", allowableValues = {"EXTERNAL_AI"}, example = "EXTERNAL_AI")
        String consentType
) {
}
