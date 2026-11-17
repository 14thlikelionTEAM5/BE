package com.likelion14th._Team.domain.auth.dto;

import java.time.LocalDateTime;

public record EmailVerifyResponse(
        String schoolEmail,
        boolean verified,
        LocalDateTime verifiedUntil
) {
}