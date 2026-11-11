package com.likelion14th._Team.domain.admission.dto;

import java.math.BigDecimal;
import java.util.List;

public record AdmissionStatsResponse(Long universityId, String name, String semester, String majorType,
        List<String> availableSemesters, long totalCount, long passCount, long failCount,
        GpaRange gpa, List<LanguageRange> languageScores, MyProfile myProfile) {
    public record GpaRange(BigDecimal scale, BigDecimal min, BigDecimal max) {}
    public record LanguageRange(String testType, BigDecimal min, BigDecimal max) {}
    public record MyLanguageScore(String testType, BigDecimal score) {}
    public record MyProfile(BigDecimal gpa, List<MyLanguageScore> languageScores) {}
}
