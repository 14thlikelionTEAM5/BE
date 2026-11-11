package com.likelion14th._Team.domain.admission.dto;

import java.math.BigDecimal;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

public record AdmissionStatsResponse(Long universityId, String name, String semester, String majorType,
        List<String> availableSemesters, long totalCount, long passCount, long failCount,
        GpaRange gpa,
        @Schema(description = "TOEFL_IBT, TOEFL_NEW, TOEFL_ITP, IELTS 순서. 미정의 시험은 마지막에 시험명순으로 반환")
        List<LanguageRange> languageScores, MyProfile myProfile) {
    public record GpaRange(BigDecimal scale, BigDecimal min, BigDecimal max) {}
    public record LanguageRange(String testType, BigDecimal min, BigDecimal max) {}
    public record MyLanguageScore(String testType, BigDecimal score) {}
    public record MyProfile(BigDecimal gpa,
            @Schema(description = "TOEFL_IBT, TOEFL_NEW, TOEFL_ITP, IELTS 순서")
            List<MyLanguageScore> languageScores) {}
}
