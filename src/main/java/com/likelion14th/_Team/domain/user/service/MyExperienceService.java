package com.likelion14th._Team.domain.user.service;

import com.likelion14th._Team.domain.admission.repository.AdmissionReportRepository;
import com.likelion14th._Team.domain.coursecase.repository.CourseCaseRepository;
import com.likelion14th._Team.domain.returnreport.entity.ReturnReportCourse;
import com.likelion14th._Team.domain.returnreport.repository.ReturnReportCourseRepository;
import com.likelion14th._Team.domain.returnreport.repository.ReturnReportRepository;
import com.likelion14th._Team.domain.user.dto.MyExperiencesResponse;
import com.likelion14th._Team.domain.user.dto.MyExperiencesResponse.AdmissionReportItem;
import com.likelion14th._Team.domain.user.dto.MyExperiencesResponse.CourseCaseItem;
import com.likelion14th._Team.domain.user.dto.MyExperiencesResponse.ReturnReportItem;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 내 등록 내역 조회 (합격·탈락 스펙, 귀국보고서, 인정 사례).
 * 500은 공통 문구를 쓰므로 별도로 감싸지 않고 GlobalExceptionHandler에 맡긴다.
 */
@Service
@RequiredArgsConstructor
public class MyExperienceService {

    private final AdmissionReportRepository admissionReportRepository;
    private final ReturnReportRepository returnReportRepository;
    private final ReturnReportCourseRepository returnReportCourseRepository;
    private final CourseCaseRepository courseCaseRepository;

    @Transactional(readOnly = true)
    public MyExperiencesResponse getMyExperiences(Long userId) {
        List<AdmissionReportItem> admissionReports = admissionReportRepository
                .findAllByUserIdWithUniversity(userId).stream()
                .map(AdmissionReportItem::from)
                .toList();

        // 수강 교과목은 한 번에 조회한 뒤 귀국보고서별로 묶는다
        Map<Long, List<ReturnReportCourse>> coursesByReportId = returnReportCourseRepository
                .findAllByUserId(userId).stream()
                .collect(Collectors.groupingBy(course -> course.getReturnReport().getId()));

        List<ReturnReportItem> returnReports = returnReportRepository
                .findAllByUserIdWithUniversity(userId).stream()
                .map(report -> ReturnReportItem.of(report, coursesByReportId.getOrDefault(report.getId(), List.of())))
                .toList();

        List<CourseCaseItem> courseCases = courseCaseRepository
                .findAllByUserIdWithDetails(userId).stream()
                .map(CourseCaseItem::from)
                .toList();

        return new MyExperiencesResponse(admissionReports, returnReports, courseCases);
    }
}