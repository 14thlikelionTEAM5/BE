package com.likelion14th._Team.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordChangeRequest(

        @NotBlank(message = "현재 비밀번호를 입력해 주세요")
        String currentPassword,

        // 가입과 같은 규칙. 글자 수 상한은 여기서, BCrypt의 72바이트 제한은 PasswordService에서 검증한다
        @NotBlank(message = "새 비밀번호를 입력해 주세요")
        @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하여야 합니다")
        String newPassword
) {
}