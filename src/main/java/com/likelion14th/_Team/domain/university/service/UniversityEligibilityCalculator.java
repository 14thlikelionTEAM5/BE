package com.likelion14th._Team.domain.university.service;

import com.likelion14th._Team.domain.university.dto.UniversityListResponse.Eligibility;
import com.likelion14th._Team.domain.university.entity.*;
import com.likelion14th._Team.global.enums.LanguageTestType;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class UniversityEligibilityCalculator {
    public Eligibility calculate(Agreement agreement, BigDecimal gpa,
            Map<LanguageTestType, BigDecimal> scores, List<AgreementLanguageRequirement> requirements) {
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
        List<String> failures = new ArrayList<>();
        List<String> unknown = new ArrayList<>();

        // 시험 종류는 대체 가능한 조건이다. 한 종류라도 모든 확인 가능한 기준을 충족하면 된다.
        boolean passed = false;
        boolean totalMetButUnknown = false;
        boolean hasComparableScore = false;
        for (var requirement : requirements) {
            BigDecimal score = scores.get(requirement.getTestType());
            if (score == null || requirement.getMinScore() == null) continue;
            hasComparableScore = true;
            if (score.compareTo(requirement.getMinScore()) >= 0) {
                boolean extraCondition = requirement.getMinSectionScore() != null
                        || hasAdditionalCondition(requirement.getConditionNote());
                if (extraCondition) totalMetButUnknown = true;
                else passed = true;
            }
        }
        if (!passed) {
            if (requirements.isEmpty()) unknown.add("LANGUAGE_REQUIREMENT_UNKNOWN");
            else if (totalMetButUnknown) unknown.add("LANGUAGE_DETAIL_NEEDS_CHECK");
            else if (!hasComparableScore) unknown.add("LANGUAGE_SCORE_NOT_PROVIDED");
            else failures.add("LANGUAGE_BELOW_MIN");
        }
        if (!failures.isEmpty()) return new Eligibility("NOT_ELIGIBLE", List.copyOf(failures),
                "어학 점수가 기준에 못 미쳐요.");
        if (!unknown.isEmpty()) return new Eligibility("NEEDS_CHECK", List.copyOf(unknown), "지원 조건을 확인해 주세요.");
        return new Eligibility("ELIGIBLE", List.of(), null);
    }
    private boolean hasAdditionalCondition(String note) {
        return note != null && !note.isBlank() && !note.strip().matches("[0-9]+(?:\\.[0-9]+)?");
    }
    private Eligibility result(String status, String reason, String message) {
        return new Eligibility(status, List.of(reason), message);
    }
}
