package com.likelion14th._Team.domain.returnreport.service;

import com.likelion14th._Team.domain.coursecase.repository.CourseCaseRepository;
import com.likelion14th._Team.domain.returnreport.dto.ReturnReportContent;
import com.likelion14th._Team.domain.returnreport.dto.ReturnReportCost;
import com.likelion14th._Team.domain.returnreport.dto.ReturnReportCourseRequest;
import com.likelion14th._Team.domain.returnreport.dto.ReturnReportSaveRequest;
import com.likelion14th._Team.domain.returnreport.dto.ReturnReportSaveResponse;
import com.likelion14th._Team.domain.returnreport.dto.ReturnReportUpdateRequest;
import com.likelion14th._Team.domain.returnreport.dto.ReturnReportVisa;
import com.likelion14th._Team.domain.returnreport.entity.ReturnReport;
import com.likelion14th._Team.domain.returnreport.entity.ReturnReportCourse;
import com.likelion14th._Team.domain.returnreport.repository.ReturnReportCourseRepository;
import com.likelion14th._Team.domain.returnreport.repository.ReturnReportRepository;
import com.likelion14th._Team.domain.studyplan.repository.StudyPlanItemRepository;
import com.likelion14th._Team.domain.university.entity.University;
import com.likelion14th._Team.domain.university.repository.UniversityRepository;
import com.likelion14th._Team.domain.user.entity.User;
import com.likelion14th._Team.domain.user.repository.UserRepository;
import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import com.likelion14th._Team.global.response.FieldErrorDetail;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 귀국보고서 저장·수정·삭제.
 * 500은 공통 문구("서버 오류가 발생했습니다")를 쓰므로 별도로 감싸지 않고 GlobalExceptionHandler에 맡긴다.
 * 수강 교과목 행을 지울 때는 딸린 인정 사례(course_case)를 먼저 지운다.
 */
@Service
@RequiredArgsConstructor
public class ReturnReportService {

    static final String UNIVERSITY_NOT_FOUND_MESSAGE = "존재하지 않는 학교입니다";
    static final String DELETE_FORBIDDEN_MESSAGE = "본인의 귀국보고서만 삭제할 수 있습니다";
    static final String INVALID_COURSE_MESSAGE = "수정할 수 없는 교과목이 포함되어 있습니다";

    private final ReturnReportRepository returnReportRepository;
    private final ReturnReportCourseRepository courseRepository;
    private final UniversityRepository universityRepository;
    private final UserRepository userRepository;
    private final StudyPlanItemRepository studyPlanItemRepository;
    private final CourseCaseRepository courseCaseRepository;

    // ===== 저장 =====

    @Transactional
    public ReturnReportSaveResponse save(Long userId, ReturnReportSaveRequest request) {
        // 토큰은 유효하지만 탈퇴 등으로 회원이 없으면 로그인이 필요한 것으로 본다
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));
        University university = universityRepository.findById(request.universityId())
                .orElseThrow(() -> new CustomException(ErrorCode.UNIVERSITY_NOT_FOUND, UNIVERSITY_NOT_FOUND_MESSAGE));
        if (returnReportRepository.existsByUserIdAndSemester(userId, request.semester())) {
            throw new CustomException(ErrorCode.RETURN_REPORT_DUPLICATED);
        }

        ReturnReport report = newReport(user, university, request.semester(), request);
        try {
            // 같은 학기 동시 요청은 유니크 제약(uk_return_report_user_semester) 위반으로 여기서 드러난다
            report = returnReportRepository.saveAndFlush(report);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.RETURN_REPORT_DUPLICATED);
        }

        List<ReturnReportCourse> courses = new ArrayList<>();
        for (ReturnReportCourseRequest course : request.courses()) {
            courses.add(courseRepository.save(newCourse(report, course)));
        }
        return ReturnReportSaveResponse.of(report, courses);
    }

    // ===== 수정 =====

    @Transactional
    public ReturnReportSaveResponse update(Long userId, Long reportId, ReturnReportUpdateRequest request) {
        ReturnReport report = findOwnedReport(userId, reportId, ErrorCode.RETURN_REPORT_FORBIDDEN.getMessage());

        Map<Long, ReturnReportCourse> existing = new LinkedHashMap<>();
        for (ReturnReportCourse course : courseRepository.findByReturnReportIdOrderByIdAsc(reportId)) {
            existing.put(course.getId(), course);
        }
        validateCourseIds(request.courses(), existing.keySet());

        // 요청에 없는 기존 행 삭제 (수강계획의 원본 연결을 먼저 끊는다)
        Set<Long> requestedIds = new HashSet<>();
        for (ReturnReportCourseRequest course : request.courses()) {
            if (course.courseId() != null) {
                requestedIds.add(course.courseId());
            }
        }
        List<ReturnReportCourse> removed = existing.values().stream()
                .filter(course -> !requestedIds.contains(course.getId()))
                .toList();
        if (!removed.isEmpty()) {
            List<Long> removedIds = removed.stream().map(ReturnReportCourse::getId).toList();
            courseCaseRepository.deleteByReturnReportCourseIds(removedIds);
            studyPlanItemRepository.clearSourceCourse(removedIds);
            courseRepository.deleteAll(removed);
        }

        List<ReturnReportCourse> result = new ArrayList<>();
        for (ReturnReportCourseRequest course : request.courses()) {
            if (course.courseId() == null) {
                result.add(courseRepository.save(newCourse(report, course)));
            } else {
                ReturnReportCourse target = existing.get(course.courseId());
                target.update(course.courseCode(), course.courseNameEn(), course.foreignCredit(),
                        course.satisfactionValue());
                result.add(target);
            }
        }

        ReturnReportCost cost = ReturnReportCost.orEmpty(request.cost());
        ReturnReportVisa visa = ReturnReportVisa.orEmpty(request.visa());
        report.updateContent(cost.flight(), cost.insurance(), cost.livingMonthly(), cost.total(),
                request.housingType(), visa.duration(), visa.type(),
                request.insuranceProvider(), request.reviewSummary());
        return ReturnReportSaveResponse.of(report, result);
    }

    /** 요청의 courseId는 이 보고서 소속이어야 하고, 요청 안에서 중복되면 안 된다 */
    private void validateCourseIds(List<ReturnReportCourseRequest> courses, Set<Long> ownedIds) {
        List<FieldErrorDetail> errors = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (int i = 0; i < courses.size(); i++) {
            Long courseId = courses.get(i).courseId();
            if (courseId == null) {
                continue;
            }
            String field = "courses[" + i + "].courseId";
            if (!ownedIds.contains(courseId)) {
                errors.add(new FieldErrorDetail(field, "이 귀국보고서의 교과목이 아닙니다"));
            } else if (!seen.add(courseId)) {
                errors.add(new FieldErrorDetail(field, "같은 교과목이 중복되었습니다"));
            }
        }
        if (!errors.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT, INVALID_COURSE_MESSAGE, errors);
        }
    }

    // ===== 삭제 =====

    @Transactional
    public void delete(Long userId, Long reportId) {
        ReturnReport report = findOwnedReport(userId, reportId, DELETE_FORBIDDEN_MESSAGE);
        deleteReport(report);
    }

    /**
     * 귀국보고서와 수강 교과목을 삭제한다. 회원 탈퇴에서도 이 메서드를 재사용한다.
     * DB의 ON DELETE에 의존하지 않고 순서대로 처리한다:
     * ① 인정 사례(course_case) ② 수강계획 항목의 원본 연결 해제(본인 포함 전체)
     * ③ return_report_course ④ return_report
     */
    @Transactional
    public void deleteReport(ReturnReport report) {
        List<Long> courseIds = courseRepository.findIdsByReturnReportId(report.getId());
        if (!courseIds.isEmpty()) {
            courseCaseRepository.deleteByReturnReportCourseIds(courseIds);
            studyPlanItemRepository.clearSourceCourse(courseIds);
            courseRepository.deleteAllByReturnReportId(report.getId());
        }
        returnReportRepository.delete(report);
    }

    // ===== 공통 =====

    /** 존재(404) → 소유자(403) 순서로 확인 */
    private ReturnReport findOwnedReport(Long userId, Long reportId, String forbiddenMessage) {
        ReturnReport report = returnReportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.RETURN_REPORT_NOT_FOUND));
        if (!report.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.RETURN_REPORT_FORBIDDEN, forbiddenMessage);
        }
        return report;
    }

    private ReturnReport newReport(User user, University university, String semester, ReturnReportContent content) {
        ReturnReportCost cost = ReturnReportCost.orEmpty(content.cost());
        ReturnReportVisa visa = ReturnReportVisa.orEmpty(content.visa());
        return ReturnReport.builder()
                .user(user)
                .university(university)
                .semester(semester)
                .costFlight(cost.flight())
                .costInsurance(cost.insurance())
                .costLivingMonthly(cost.livingMonthly())
                .costTotal(cost.total())
                .housingType(content.housingType())
                .visaDuration(visa.duration())
                .visaType(visa.type())
                .insuranceProvider(content.insuranceProvider())
                .reviewSummary(content.reviewSummary())
                .build();
    }

    private ReturnReportCourse newCourse(ReturnReport report, ReturnReportCourseRequest course) {
        return ReturnReportCourse.builder()
                .returnReport(report)
                .courseCode(course.courseCode())
                .courseNameEn(course.courseNameEn())
                .foreignCredit(course.foreignCredit())
                .satisfaction(course.satisfactionValue())
                .build();
    }
}
