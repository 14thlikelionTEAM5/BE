package com.likelion14th._Team.domain.university.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

public record UniversityListResponse(String semester, int totalCount, List<School> universities) {
    public record LanguageRequirement(String testType, BigDecimal minScore,
            @Schema(description = "각 영역에 공통으로 적용하는 최소 점수. null이면 구조화된 영역별 기준 없음")
            BigDecimal minSectionScore,
            @Schema(description = "어학 추가 조건 원문. 자동 판정할 수 없는 조건은 확인 필요로 처리")
            String conditionNote) {}
    public record Eligibility(String status, List<String> reasonCodes, String message) {}
    public record School(Long universityId, String name, String country, Boolean isEquis, Boolean isAacsb,
            String creditSystem, Long agreementId, Integer quota, BigDecimal minGpa,
            List<LanguageRequirement> languageRequirements, String costSummary, boolean hasExperienceData,
            LocalDate announcedAt, List<String> semesterChanges, Eligibility eligibility, boolean isBookmarked) {}
}
