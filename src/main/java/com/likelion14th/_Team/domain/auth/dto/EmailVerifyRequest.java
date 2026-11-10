package com.likelion14th._Team.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** 6자리 숫자 검사는 명세의 별도 문구(인증 코드는 6자리 숫자여야 합니다)를 내려주기 위해 서비스에서 한다 */
public record EmailVerifyRequest(

        @NotBlank(message = "학교 메일을 입력해 주세요")
        @Email(message = "이메일 형식이 올바르지 않습니다")
        String schoolEmail,

        @NotBlank(message = "인증 코드를 입력해 주세요")
        String code
) {
}