package com.likelion14th._Team.domain.bookmark.service;

import com.likelion14th._Team.domain.bookmark.dto.BookmarkCreateResponse;
import com.likelion14th._Team.domain.bookmark.dto.BookmarkDeleteResponse;
import com.likelion14th._Team.domain.bookmark.entity.Bookmark;
import com.likelion14th._Team.domain.bookmark.repository.BookmarkRepository;
import com.likelion14th._Team.domain.university.repository.UniversityRepository;
import com.likelion14th._Team.domain.user.repository.UserRepository;
import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import java.sql.SQLException;
import com.likelion14th._Team.domain.bookmark.dto.BookmarkListResponse;
import com.likelion14th._Team.domain.university.dto.UniversityListResponse.LanguageRequirement;
import com.likelion14th._Team.domain.university.entity.Agreement;
import com.likelion14th._Team.domain.university.entity.AgreementLanguageRequirement;
import com.likelion14th._Team.domain.university.repository.UniversityQueryRepository;
import com.likelion14th._Team.domain.university.service.UniversityEligibilityCalculator;
import com.likelion14th._Team.global.enums.LanguageTestType;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookmarkService {
    private final BookmarkRepository bookmarks;
    private final UniversityRepository universities;
    private final UserRepository users;
    private final UniversityQueryRepository universityQueries;
    private final UniversityEligibilityCalculator calculator;

    @Transactional
    public BookmarkCreateResponse create(Long userId, Long universityId) {
        if (userId == null) throw new CustomException(ErrorCode.UNAUTHORIZED);
        var university = universities.findById(universityId).orElseThrow(() ->
                new CustomException(ErrorCode.UNIVERSITY_NOT_FOUND, "존재하지 않는 학교입니다"));
        if (bookmarks.existsByUserIdAndUniversityId(userId, universityId)) {
            throw new CustomException(ErrorCode.BOOKMARK_DUPLICATED);
        }
        try {
            bookmarks.saveAndFlush(Bookmark.builder()
                    .user(users.getReferenceById(userId)).university(university).build());
        } catch (DataIntegrityViolationException exception) {
            // 사전 조회를 동시에 통과한 요청도 DB 유일 제약으로 막는다.
            // 외래키 위반 등 다른 무결성 오류를 중복 찜으로 잘못 분류하지 않는다.
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof SQLException sql && sql.getErrorCode() == 1062) {
                    throw new CustomException(ErrorCode.BOOKMARK_DUPLICATED);
                }
            }
            throw exception;
        }
        return new BookmarkCreateResponse(universityId, true);
    }
    @Transactional
    public BookmarkDeleteResponse delete(Long userId, Long universityId) {
        if (userId == null) throw new CustomException(ErrorCode.UNAUTHORIZED);
        if (!universities.existsById(universityId)) {
            throw new CustomException(ErrorCode.UNIVERSITY_NOT_FOUND, "존재하지 않는 학교입니다");
        }
        // 사용자 조건과 삭제를 단일 SQL로 처리하여 다른 사용자의 찜을 건드리지 않는다.
        // 동일 찜을 동시에 해제해도 실제 삭제된 요청만 성공한다.
        if (bookmarks.deleteForUser(userId, universityId) == 0) {
            throw new CustomException(ErrorCode.BOOKMARK_NOT_FOUND);
        }
        return new BookmarkDeleteResponse(universityId, false);
    }

    @Transactional(readOnly = true)
    public BookmarkListResponse list(Long userId) {
        if (userId == null) throw new CustomException(ErrorCode.UNAUTHORIZED);
        var semesters = universityQueries.semesters();
        String semester = semesters.isEmpty() ? null : semesters.getFirst();
        var saved = bookmarks.findAllForUser(userId);
        if (saved.isEmpty()) return new BookmarkListResponse(semester, 0, List.of());

        var ids = saved.stream().map(b -> b.getUniversity().getId()).toList();
        var agreements = universityQueries.agreementsForSchools(
                semester == null ? List.of() : List.of(semester), ids);
        Map<Long, Agreement> bySchool = agreements.stream().collect(Collectors.toMap(
                a -> a.getUniversity().getId(), Function.identity()));
        Map<Long, List<AgreementLanguageRequirement>> requirements = universityQueries.requirementsForAgreements(
                agreements.stream().map(Agreement::getId).toList()).stream()
                .collect(Collectors.groupingBy(r -> r.getAgreement().getId()));
        var applicant = universityQueries.applicant(userId);
        BigDecimal gpa = applicant.isEmpty() ? null : applicant.getFirst().gpa();
        Map<LanguageTestType, BigDecimal> scores = new EnumMap<>(LanguageTestType.class);
        applicant.stream().filter(a -> a.testType() != null && a.score() != null)
                .forEach(a -> scores.put(a.testType(), a.score()));
        var schools = saved.stream().map(bookmark -> {
            var university = bookmark.getUniversity();
            Agreement agreement = bySchool.get(university.getId());
            var language = agreement == null ? List.<AgreementLanguageRequirement>of()
                    : requirements.getOrDefault(agreement.getId(), List.of());
            var displayed = agreement == null ? null : language.stream()
                    .sorted(Comparator.comparing(r -> r.getTestType().ordinal()))
                    .map(r -> new LanguageRequirement(r.getTestType().name(), r.getMinScore())).toList();
            return new BookmarkListResponse.School(university.getId(), university.getName(), university.getCountry(),
                    agreement == null ? null : agreement.getId(), agreement == null ? null : agreement.getQuota(),
                    agreement == null ? null : agreement.getMinGpa(), displayed,
                    calculator.calculate(agreement, gpa, scores, language), true);
        }).toList();
        return new BookmarkListResponse(semester, schools.size(), schools);
    }

}
