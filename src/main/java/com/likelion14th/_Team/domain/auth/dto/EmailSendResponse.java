package com.likelion14th._Team.domain.auth.dto;

import java.time.LocalDateTime;

public record EmailSendResponse(
        String schoolEmail,
        LocalDateTime expiresAt,
        LocalDateTime resendAvailableAt
) {
}