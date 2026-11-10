package com.likelion14th._Team.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailSendRequest(

        @NotBlank(message = "학교 메일을 입력해 주세요")
        @Email(message = "이메일 형식이 올바르지 않습니다")
        String schoolEmail
) {
}