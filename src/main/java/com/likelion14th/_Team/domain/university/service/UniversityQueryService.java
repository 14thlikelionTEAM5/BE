package com.likelion14th._Team.domain.university.service;

import com.likelion14th._Team.domain.university.dto.*;
import com.likelion14th._Team.domain.university.dto.UniversityListResponse.*;
import com.likelion14th._Team.domain.university.entity.*;
import com.likelion14th._Team.domain.university.repository.UniversityQueryRepository;
import com.likelion14th._Team.global.enums.LanguageTestType;
import com.likelion14th._Team.global.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UniversityQueryService {
    private final UniversityQueryRepository repository;
    private final UniversityEligibilityCalculator calculator;
    private final UniversityCardAssembler cardAssembler;

    public UniversityDetailResponse detail(Long userId, Long universityId, String requestedSemester) {
        if (userId == null) {
            throw new CustomException(ErrorCode.UNIVERSITY_COUNTRIES_LOGIN_REQUIRED);
        }
        University u = repository.university(universityId).orElseThrow(() ->
                new CustomException(ErrorCode.UNIVERSITY_NOT_FOUND, "존재하지 않는 학교입니다"));
        var agreements = repository.schoolAgreements(universityId);
        var available = agreements.stream().map(Agreement::getSemester).distinct().toList();
        String semester = requestedSemester != null ? requestedSemester : available.isEmpty() ? null : available.getFirst();
        Agreement a = agreements.stream().filter(item -> Objects.equals(item.getSemester(), semester)).findFirst().orElse(null);
        var language = a == null ? List.<AgreementLanguageRequirement>of() : repository.agreementRequirements(a.getId());
        Map<LanguageTestType, BigDecimal> scores = new EnumMap<>(LanguageTestType.class);
        repository.scores(userId).forEach(s -> scores.put(s.getTestType(), s.getScore()));
        BigDecimal gpa = repository.profile(userId).map(p -> p.getGpa()).orElse(null);
        Eligibility eligibility = calculator.calculate(a, gpa, scores, language);
        var details = a == null ? null : new UniversityDetailResponse.AgreementDetail(a.getId(), a.getQuota(),
                a.getExchangePeriod(), a.getMinGpa(), a.getCostSummary(), a.getBenefitSummary(), a.getRemarkSummary(),
                a.getCourseRestriction(), a.isBusinessOnly(), a.isOpen(), a.getAnnouncedAt(), a.getImportedAt(),
                a.getSourceUrl(), UniversityCardAssembler.languageRequirements(language));
        Long planId = repository.studyPlanId(userId, universityId).orElse(null);
        return new UniversityDetailResponse(u.getId(), u.getName(), u.getCountry(), u.getIsEquis(), u.getIsAacsb(),
                u.getCreditSystem().name(), u.getCourseListUrl(), semester, available, details, eligibility,
                repository.bookmarks(userId).contains(universityId), planId != null, planId);
    }

    public UniversityCountriesResponse countries(Long userId) {
        if (userId == null) {
            throw new CustomException(ErrorCode.UNIVERSITY_COUNTRIES_LOGIN_REQUIRED);
        }
        return new UniversityCountriesResponse(List.copyOf(repository.countries()));
    }

    public UniversityListResponse list(Long userId, UniversitySearch search) {
        if (userId == null) {
            throw new CustomException(ErrorCode.UNIVERSITY_LOGIN_REQUIRED);
        }
        var semesters = cardAssembler.semesters();
        String semester = semesters.current();
        var schools = repository.schools(userId, search, semester);
        if (schools.isEmpty()) return new UniversityListResponse(semester, 0, List.of());
        var cards = cardAssembler.cards(userId, semesters.comparison(),
                schools.stream().map(row -> row.university()).toList());
        List<School> result = new ArrayList<>();
        for (var row : schools) {
            var card = cards.get(row.university().getId());
            University u = card.university();
            Agreement a = card.agreement();
            var eligibility = card.eligibility();
            if (search.eligibleOnly() && !eligibility.status().equals("ELIGIBLE")) continue;
            result.add(new School(card.universityId(), card.name(), card.country(), u.getIsEquis(), u.getIsAacsb(),
                    u.getCreditSystem().name(), card.agreementId(), card.quota(),
                    card.minGpa(), card.languageRequirements(),
                    a == null ? null : a.getCostSummary(), row.experienceData(),
                    a == null ? null : a.getAnnouncedAt(), changes(a, card.previous(), semesters.previous() != null),
                    eligibility, row.bookmarked()));
        }
        Comparator<School> order = Comparator.comparing(School::name, String.CASE_INSENSITIVE_ORDER).thenComparing(School::universityId);
        if (search.sort().equals("COUNTRY")) order = Comparator.comparing(School::country).thenComparing(order);
        result.sort(order);
        return new UniversityListResponse(semester, result.size(), List.copyOf(result));
    }
    static List<String> changes(Agreement current, Agreement previous, boolean hasPreviousSemester) {
        if (current == null) return List.of("RECRUITMENT_STOPPED");
        List<String> result = new ArrayList<>();
        if (hasPreviousSemester) {
            if (previous == null) result.add("NEW");
            else if (!Objects.equals(current.getQuota(), previous.getQuota())) result.add("QUOTA_CHANGED");
        }
        if (!current.isOpen()) result.add("RECRUITMENT_STOPPED");
        return List.copyOf(result);
    }
}
