package com.likelion14th._Team.domain.university.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import com.likelion14th._Team.domain.university.dto.UniversityListResponse.Eligibility;
import com.likelion14th._Team.domain.university.dto.UniversityListResponse.LanguageRequirement;

public record UniversityDetailResponse(Long universityId, String name, String country, Boolean isEquis,
        Boolean isAacsb, String creditSystem, String courseListUrl, String semester,
        List<String> availableSemesters, AgreementDetail agreement, Eligibility eligibility,
        boolean isBookmarked, boolean hasStudyPlan, Long studyPlanId) {
    public record AgreementDetail(Long agreementId, Integer quota, String exchangePeriod, BigDecimal minGpa,
            String costSummary, String benefitSummary, String remarkSummary, String courseRestriction,
            Boolean isBusinessOnly, Boolean isOpen, LocalDate announcedAt, LocalDateTime importedAt,
            String sourceUrl, List<LanguageRequirement> languageRequirements) {}
}
