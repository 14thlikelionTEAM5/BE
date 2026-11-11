package com.likelion14th._Team.domain.user.dto;

import com.likelion14th._Team.domain.user.entity.Consent;
import com.likelion14th._Team.global.enums.ConsentType;
import java.time.LocalDateTime;

public record ConsentResponse(
        ConsentType consentType,
        LocalDateTime consentedAt
) {

    public static ConsentResponse from(Consent consent) {
        return new ConsentResponse(consent.getConsentType(), consent.getConsentedAt());
    }
}
