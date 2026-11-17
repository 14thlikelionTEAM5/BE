package com.likelion14th._Team.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "학교 메일을 입력해 주세요")
        @Email(message = "이메일 형식이 올바르지 않습니다")
        String schoolEmail,

        @NotBlank(message = "비밀번호를 입력해 주세요")
        String password
) {
}