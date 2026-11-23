package com.likelion14th._Team.domain.university.service;

import com.likelion14th._Team.domain.university.dto.UniversityListResponse.Eligibility;
import com.likelion14th._Team.domain.university.entity.*;
import com.likelion14th._Team.domain.user.entity.LanguageSectionScores;
import com.likelion14th._Team.global.enums.LanguageTestType;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class UniversityEligibilityCalculator {
    public Eligibility calculate(Agreement agreement, BigDecimal gpa,
            Map<LanguageTestType, LanguageScore> scores, List<AgreementLanguageRequirement> requirements) {
        if (agreement == null) return result("NOT_ELIGIBLE", "NO_CURRENT_AGREEMENT", "이번 학기 모집 없음");
        if (!agreement.isOpen()) return result("NOT_ELIGIBLE", "RECRUITMENT_STOPPED", "이번 학기 모집 없음");
        // 공고 확인 → 사용자 GPA → 학교 GPA 기준 → 어학 순서로 판정한다.
        // GPA를 판정할 수 없으면 어학 점수 미달로 결과를 덮어쓰지 않는다.
        // GPA는 필수 입력이다. 프로필 행이 없는 기존/비정상 데이터를 위한 방어 분기다.
        if (gpa == null) return result("NEEDS_CHECK", "GPA_NOT_PROVIDED", "지원 조건을 확인해 주세요.");
        if (agreement.getMinGpa() == null) {
            return result("NEEDS_CHECK", "GPA_REQUIREMENT_UNKNOWN", "지원 조건을 확인해 주세요.");
        }
        if (gpa.compareTo(agreement.getMinGpa()) < 0) {
            return result("NOT_ELIGIBLE", "GPA_BELOW_MIN", "학점이 기준에 못 미쳐요.");
        }
        Set<String> failures = new LinkedHashSet<>();
        Set<String> unknown = new LinkedHashSet<>();
        boolean hasComparableScore = false;
        for (var requirement : requirements) {
            LanguageScore score = scores.get(requirement.getTestType());
            if (score == null || score.score() == null || requirement.getMinScore() == null) continue;
            hasComparableScore = true;
            if (score.score().compareTo(requirement.getMinScore()) < 0) {
                failures.add("LANGUAGE_BELOW_MIN");
                continue;
            }
            BigDecimal minimum = requirement.getMinSectionScore();
            if (minimum != null) {
                LanguageSectionScores sections = score.sectionScores();
                if (sections == null || !sections.isComplete()) {
                    unknown.add("LANGUAGE_SECTION_SCORE_NOT_PROVIDED");
                    continue;
                }
                if (sections.values().stream().anyMatch(value -> value.compareTo(minimum) < 0)) {
                    failures.add("LANGUAGE_SECTION_BELOW_MIN");
                    continue;
                }
            }
            if (hasAdditionalCondition(requirement)) {
                unknown.add("LANGUAGE_DETAIL_NEEDS_CHECK");
                continue;
            }
            // 시험은 대체 조건: 다른 시험의 미달·누락보다 한 시험의 완전한 충족이 우선한다.
            return new Eligibility("ELIGIBLE", List.of(), null);
        }
        if (requirements.isEmpty()) unknown.add("LANGUAGE_REQUIREMENT_UNKNOWN");
        else if (!hasComparableScore) unknown.add("LANGUAGE_SCORE_NOT_PROVIDED");
        // 하나의 시험이 미달해도 다른 시험의 조건이 미확정이면 전체 탈락으로 단정하지 않는다.
        if (!unknown.isEmpty()) {
            String message;
            if (unknown.contains("LANGUAGE_SECTION_SCORE_NOT_PROVIDED")) {
                message = "총점은 충족하지만 영역별 점수가 없어 지원 자격을 확인해야 해요.";
            } else if (unknown.contains("LANGUAGE_DETAIL_NEEDS_CHECK")) {
                message = "어학 총점 외 추가 지원 조건을 확인해 주세요.";
            } else {
                message = scores.isEmpty() && unknown.contains("LANGUAGE_SCORE_NOT_PROVIDED")
                        ? "어학 성적이 없어 지원 자격을 확인해야 해요." : "지원 조건을 확인해 주세요.";
            }
            return new Eligibility("NEEDS_CHECK", List.copyOf(unknown), message);
        }
        if (!failures.isEmpty()) return new Eligibility("NOT_ELIGIBLE", List.copyOf(failures),
                failures.contains("LANGUAGE_SECTION_BELOW_MIN")
                        ? "어학 영역별 점수가 기준에 못 미쳐요." : "어학 점수가 기준에 못 미쳐요.");
        return new Eligibility("ELIGIBLE", List.of(), null);
    }
    private static final Pattern SECTION_NOTE = Pattern.compile(
            "([0-9]+(?:\\.[0-9]+)?)\\s*\\(\\s*영역별\\s*([0-9]+(?:\\.[0-9]+)?)\\s*이상\\s*\\)");

    private boolean hasAdditionalCondition(AgreementLanguageRequirement requirement) {
        String note = requirement.getConditionNote();
        if (note == null || note.isBlank() || note.strip().matches("[0-9]+(?:\\.[0-9]+)?")) return false;
        var matcher = SECTION_NOTE.matcher(note.strip());
        // 구조화된 총점·영역별 기준을 반복한 문구만 확인 완료로 처리한다.
        // R:22 W:22 등 별도 조건이나 불일치하는 원문은 계속 수동 확인이 필요하다.
        return requirement.getMinSectionScore() == null || !matcher.matches()
                || new BigDecimal(matcher.group(1)).compareTo(requirement.getMinScore()) != 0
                || new BigDecimal(matcher.group(2)).compareTo(requirement.getMinSectionScore()) != 0;
    }

    public record LanguageScore(BigDecimal score, LanguageSectionScores sectionScores) {}

    private Eligibility result(String status, String reason, String message) {
        return new Eligibility(status, List.of(reason), message);
    }
}
