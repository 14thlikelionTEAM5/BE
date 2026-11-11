package com.likelion14th._Team.domain.admission.service;

import com.likelion14th._Team.domain.admission.dto.*;
import com.likelion14th._Team.domain.admission.repository.AdmissionReportStatsRepository;
import com.likelion14th._Team.domain.university.repository.UniversityRepository;
import com.likelion14th._Team.global.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdmissionReportStatsService {
    private final AdmissionReportStatsRepository repository;
    private final UniversityRepository universities;

    public AdmissionReportListResponse list(Long userId, long universityId, String result) {
        if (userId == null || !repository.userExists(userId)) throw new CustomException(ErrorCode.UNAUTHORIZED);
        if (!universities.existsById(universityId)) {
            throw new CustomException(ErrorCode.UNIVERSITY_NOT_FOUND, "존재하지 않는 학교입니다");
        }
        var reports = repository.reports(universityId, result);
        return new AdmissionReportListResponse(universityId, reports.size(), reports);
    }
    public AdmissionStatsResponse stats(Long userId, AdmissionReportFilter filter) {
        if (userId == null || !repository.userExists(userId)) throw new CustomException(ErrorCode.UNAUTHORIZED);
        var university=universities.findById(filter.universityId()).orElseThrow(() ->
                new CustomException(ErrorCode.UNIVERSITY_NOT_FOUND,"존재하지 않는 학교입니다"));
        var counts=repository.counts(filter);
        long pass=counts.getOrDefault("PASS",0L), fail=counts.getOrDefault("FAIL",0L);
        return new AdmissionStatsResponse(university.getId(),university.getName(),filter.semester(),filter.majorType(),
                repository.semesters(filter.universityId()),pass+fail,pass,fail,repository.gpa(filter),
                repository.languageScores(filter),repository.myProfile(userId));
    }
}
