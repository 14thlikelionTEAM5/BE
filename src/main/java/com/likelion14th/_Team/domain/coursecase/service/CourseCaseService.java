package com.likelion14th._Team.domain.coursecase.service;

import com.likelion14th._Team.domain.course.entity.HomeCourse;
import com.likelion14th._Team.domain.course.repository.HomeCourseRepository;
import com.likelion14th._Team.domain.coursecase.dto.CourseCaseRequest;
import com.likelion14th._Team.domain.coursecase.dto.CourseCaseResponse;
import com.likelion14th._Team.domain.coursecase.entity.CourseCase;
import com.likelion14th._Team.domain.coursecase.repository.CourseCaseRepository;
import com.likelion14th._Team.domain.returnreport.entity.ReturnReportCourse;
import com.likelion14th._Team.domain.returnreport.repository.ReturnReportCourseRepository;
import com.likelion14th._Team.domain.user.entity.User;
import com.likelion14th._Team.domain.user.repository.UserRepository;
import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인정 사례 저장·수정·삭제.
 * 500은 공통 문구("서버 오류가 발생했습니다")를 쓰므로 별도로 감싸지 않고 GlobalExceptionHandler에 맡긴다.
 */
@Service
@RequiredArgsConstructor
public class CourseCaseService {

    static final String HOME_COURSE_NOT_FOUND_MESSAGE = "존재하지 않는 본교 과목입니다";
    static final String DELETE_FORBIDDEN_MESSAGE = "본인의 인정 사례만 삭제할 수 있습니다";

    private final CourseCaseRepository courseCaseRepository;
    private final ReturnReportCourseRepository returnReportCourseRepository;
    private final HomeCourseRepository homeCourseRepository;
    private final UserRepository userRepository;

    // ===== 저장 =====

    @Transactional
    public CourseCaseResponse save(Long userId, CourseCaseRequest request) {
        // 토큰은 유효하지만 탈퇴 등으로 회원이 없으면 로그인이 필요한 것으로 본다
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));
        ReturnReportCourse course = findOwnedCourse(userId, request.returnReportCourseId());
        if (courseCaseRepository.existsByReturnReportCourseId(course.getId())) {
            throw new CustomException(ErrorCode.COURSE_CASE_DUPLICATED);
        }
        HomeCourse homeCourse = findHomeCourse(request.resolvedHomeCourseId());

        CourseCase courseCase = CourseCase.builder()
                .returnReportCourse(course)
                .homeCourse(homeCourse)
                .recognitionType(request.recognitionType())
                .recognizedCredit(request.recognizedCredit())
                .majorType(user.getMajorType())
                .semester(course.getReturnReport().getSemester())
                .build();
        try {
            // 같은 과목 동시 요청은 유니크 제약(uk_course_case_return_report_course) 위반으로 여기서 드러난다
            courseCase = courseCaseRepository.saveAndFlush(courseCase);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.COURSE_CASE_DUPLICATED);
        }
        return CourseCaseResponse.of(courseCase);
    }

    // ===== 수정 =====

    @Transactional
    public CourseCaseResponse update(Long userId, Long caseId, CourseCaseRequest request) {
        CourseCase courseCase = findOwnedCase(userId, caseId, ErrorCode.COURSE_CASE_FORBIDDEN.getMessage());

        ReturnReportCourse course = courseCase.getReturnReportCourse();
        // 대상 과목이 바뀐 경우에만 새 과목의 본인 소유와 기존 인정 사례 유무를 확인한다
        if (!course.getId().equals(request.returnReportCourseId())) {
            course = findOwnedCourse(userId, request.returnReportCourseId());
            if (courseCaseRepository.existsByReturnReportCourseIdAndIdNot(course.getId(), courseCase.getId())) {
                throw new CustomException(ErrorCode.COURSE_CASE_DUPLICATED);
            }
        }
        HomeCourse homeCourse = findHomeCourse(request.resolvedHomeCourseId());

        courseCase.update(course, homeCourse, request.recognitionType(), request.recognizedCredit(),
                course.getReturnReport().getSemester());
        try {
            courseCaseRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.COURSE_CASE_DUPLICATED);
        }
        return CourseCaseResponse.of(courseCase);
    }

    // ===== 삭제 =====

    @Transactional
    public void delete(Long userId, Long caseId) {
        CourseCase courseCase = findOwnedCase(userId, caseId, DELETE_FORBIDDEN_MESSAGE);
        courseCaseRepository.delete(courseCase);
    }

    // ===== 공통 =====

    /** 존재(404) → 소유자(403) 순서로 확인. 소유자는 과목이 속한 귀국보고서의 user_id */
    private ReturnReportCourse findOwnedCourse(Long userId, Long courseId) {
        ReturnReportCourse course = returnReportCourseRepository.findById(courseId)
                .orElseThrow(() -> new CustomException(ErrorCode.RETURN_REPORT_COURSE_NOT_FOUND));
        if (!course.getReturnReport().getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.RETURN_REPORT_COURSE_FORBIDDEN);
        }
        return course;
    }

    /** 존재(404) → 소유자(403) 순서로 확인 */
    private CourseCase findOwnedCase(Long userId, Long caseId, String forbiddenMessage) {
        CourseCase courseCase = courseCaseRepository.findById(caseId)
                .orElseThrow(() -> new CustomException(ErrorCode.COURSE_CASE_NOT_FOUND));
        if (!courseCase.getReturnReportCourse().getReturnReport().getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.COURSE_CASE_FORBIDDEN, forbiddenMessage);
        }
        return courseCase;
    }

    /** 교양이면 id가 null이라 조회하지 않는다 */
    private HomeCourse findHomeCourse(Long homeCourseId) {
        if (homeCourseId == null) {
            return null;
        }
        return homeCourseRepository.findById(homeCourseId)
                .orElseThrow(() -> new CustomException(ErrorCode.HOME_COURSE_NOT_FOUND,
                        HOME_COURSE_NOT_FOUND_MESSAGE));
    }
}