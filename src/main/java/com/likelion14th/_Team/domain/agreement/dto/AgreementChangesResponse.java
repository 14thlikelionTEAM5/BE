package com.likelion14th._Team.domain.agreement.dto;

import java.time.LocalDate;

public record AgreementChangesResponse(String semester, String previousSemester, LocalDate standardDate,
        Long newCount, Long quotaChangedCount, Long closedCount) {
}
