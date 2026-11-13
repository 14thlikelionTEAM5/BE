package com.likelion14th._Team.domain.university.service;

import com.likelion14th._Team.domain.university.dto.UniversityListResponse.Eligibility;
import com.likelion14th._Team.domain.university.dto.UniversityListResponse.LanguageRequirement;
import com.likelion14th._Team.domain.university.entity.Agreement;
import com.likelion14th._Team.domain.university.entity.AgreementLanguageRequirement;
import com.likelion14th._Team.domain.university.entity.University;
import com.likelion14th._Team.domain.university.repository.UniversityQueryRepository;
import com.likelion14th._Team.global.enums.LanguageTestType;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 협정교·찜 목록의 공통 정보를 한 요청 안에서 일괄 조회하고 조립한다. */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UniversityCardAssembler {
    private final UniversityQueryRepository repository;
    private final UniversityEligibilityCalculator calculator;

    public Semesters semesters() {
        List<String> semesters = repository.semesters();
        return new Semesters(semesters.isEmpty() ? null : semesters.getFirst(),
                semesters.size() < 2 ? null : semesters.get(1));
    }

    /** 호출자가 필요한 학기만 전달한다. 찜 목록은 직전 학기 공고를 조회하지 않는다. */
    public Map<Long, Card> cards(Long userId, List<String> semesters, List<University> schools) {
        if (schools.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = schools.stream().map(University::getId).distinct().toList();
        List<Agreement> agreements = repository.agreementsForSchools(semesters, ids);
        String currentSemester = semesters.isEmpty() ? null : semesters.getFirst();
        String previousSemester = semesters.size() < 2 ? null : semesters.get(1);
        Map<Long, Agreement> current = bySchool(agreements, currentSemester);
        Map<Long, Agreement> previous = bySchool(agreements, previousSemester);
        var requirements = repository.requirementsForAgreements(
                current.values().stream().map(Agreement::getId).toList()).stream()
                .collect(Collectors.groupingBy(r -> r.getAgreement().getId()));
        var applicant = repository.applicant(userId);
        BigDecimal gpa = applicant.isEmpty() ? null : applicant.getFirst().gpa();
        Map<LanguageTestType, BigDecimal> scores = new EnumMap<>(LanguageTestType.class);
        applicant.stream().filter(row -> row.testType() != null && row.score() != null)
                .forEach(row -> scores.put(row.testType(), row.score()));

        Map<Long, Card> result = new LinkedHashMap<>();
        for (University school : schools) {
            Agreement agreement = current.get(school.getId());
            List<AgreementLanguageRequirement> language = agreement == null ? List.of()
                    : requirements.getOrDefault(agreement.getId(), List.of());
            result.put(school.getId(), new Card(school, agreement, previous.get(school.getId()),
                    agreement == null ? null : languageRequirements(language),
                    calculator.calculate(agreement, gpa, scores, language)));
        }
        return Map.copyOf(result);
    }

    private Map<Long, Agreement> bySchool(List<Agreement> agreements, String semester) {
        return agreements.stream().filter(a -> Objects.equals(a.getSemester(), semester))
                .collect(Collectors.toMap(a -> a.getUniversity().getId(), Function.identity()));
    }

    static List<LanguageRequirement> languageRequirements(List<AgreementLanguageRequirement> requirements) {
        return requirements.stream().sorted(Comparator.comparing(r -> r.getTestType().ordinal()))
                .map(r -> new LanguageRequirement(r.getTestType().name(), r.getMinScore())).toList();
    }

    public record Semesters(String current, String previous) {
        public List<String> currentOnly() {
            return current == null ? List.of() : List.of(current);
        }

        public List<String> comparison() {
            return previous == null ? currentOnly() : List.of(current, previous);
        }
    }

    public record Card(University university, Agreement agreement, Agreement previous,
                       List<LanguageRequirement> languageRequirements, Eligibility eligibility) {
        public Long universityId() { return university.getId(); }
        public String name() { return university.getName(); }
        public String country() { return university.getCountry(); }
        public Long agreementId() { return agreement == null ? null : agreement.getId(); }
        public Integer quota() { return agreement == null ? null : agreement.getQuota(); }
        public BigDecimal minGpa() { return agreement == null ? null : agreement.getMinGpa(); }
    }
}
