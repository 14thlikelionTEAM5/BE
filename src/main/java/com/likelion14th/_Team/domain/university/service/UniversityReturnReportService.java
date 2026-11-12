package com.likelion14th._Team.domain.university.service;

import com.likelion14th._Team.domain.returnreport.repository.ReturnReportRepository;
import com.likelion14th._Team.domain.university.repository.UniversityRepository;
import com.likelion14th._Team.domain.university.dto.UniversityReturnReportsResponse;
import com.likelion14th._Team.domain.university.dto.UniversityReturnReportsResponse.Report;
import com.likelion14th._Team.global.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UniversityReturnReportService {
    private final UniversityRepository universities;
    private final ReturnReportRepository reports;

    @Transactional(readOnly = true)
    public UniversityReturnReportsResponse reports(long universityId) {
        if (!universities.existsById(universityId)) {
            throw new CustomException(ErrorCode.UNIVERSITY_NOT_FOUND, "존재하지 않는 학교입니다");
        }
        var items = reports.findByUniversityIdOrderBySemesterDescCreatedAtDescIdDesc(universityId)
                .stream().map(Report::from).toList();
        // 저장된 구조화 정보와 요약만 사용한다. 작성자와 원문 보고서, AI 클라이언트에는 접근하지 않는다.
        return new UniversityReturnReportsResponse(universityId, items.size(), items);
    }
}
