package com.likelion14th._Team.domain.experience.dto;

import com.likelion14th._Team.global.enums.DuplicateReason;

public record DuplicateCheckResponse(
        boolean available,
        DuplicateReason reason
) {

    public static DuplicateCheckResponse ok() {
        return new DuplicateCheckResponse(true, null);
    }

    public static DuplicateCheckResponse unavailable(DuplicateReason reason) {
        return new DuplicateCheckResponse(false, reason);
    }
}