package com.likelion14th._Team.domain.university.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record UniversityListResponse(String semester, int totalCount, List<School> universities) {
    public record LanguageRequirement(String testType, BigDecimal minScore) {}
    public record Eligibility(String status, List<String> reasonCodes, String message) {}
    public record School(Long universityId, String name, String country, Boolean isEquis, Boolean isAacsb,
            String creditSystem, Long agreementId, Integer quota, BigDecimal minGpa,
            List<LanguageRequirement> languageRequirements, String costSummary, boolean hasExperienceData,
            LocalDate announcedAt, List<String> semesterChanges, Eligibility eligibility, boolean isBookmarked) {}
}
