package com.likelion14th._Team.domain.admission.service;

import com.likelion14th._Team.domain.admission.dto.AdmissionReportRequest;
import com.likelion14th._Team.domain.admission.dto.AdmissionReportResponse;
import com.likelion14th._Team.domain.admission.entity.AdmissionReport;
import com.likelion14th._Team.domain.admission.repository.AdmissionReportRepository;
import com.likelion14th._Team.domain.university.entity.University;
import com.likelion14th._Team.domain.university.repository.UniversityRepository;
import com.likelion14th._Team.domain.user.entity.User;
import com.likelion14th._Team.domain.user.entity.UserProfile;
import com.likelion14th._Team.domain.user.repository.UserProfileRepository;
import com.likelion14th._Team.domain.user.repository.UserRepository;
import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 합격·탈락 스펙 저장·수정·삭제.
 * 500은 공통 문구("서버 오류가 발생했습니다")를 쓰므로 별도로 감싸지 않고 GlobalExceptionHandler에 맡긴다.
 */
@Service
@RequiredArgsConstructor
public class AdmissionReportService {

    static final String UNIVERSITY_NOT_FOUND_MESSAGE = "존재하지 않는 학교입니다";
    static final String DELETE_FORBIDDEN_MESSAGE = "본인의 스펙만 삭제할 수 있습니다";

    private final AdmissionReportRepository admissionReportRepository;
    private final UniversityRepository universityRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    // ===== 저장 =====

    @Transactional
    public AdmissionReportResponse save(Long userId, AdmissionReportRequest request) {
        // 토큰은 유효하지만 탈퇴 등으로 회원이 없으면 로그인이 필요한 것으로 본다
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));
        University university = findUniversity(request.universityId());
        if (admissionReportRepository.existsByUserIdAndUniversityIdAndSemesterAndResult(
                userId, university.getId(), request.semester(), request.result())) {
            throw new CustomException(ErrorCode.ADMISSION_REPORT_DUPLICATED);
        }

        // 누적 학점은 프로필, 전공 구분은 회원에서 등록 시점 값을 복사한다 (요청으로 받지 않는다)
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_ERROR));

        AdmissionReport report = AdmissionReport.builder()
                .user(user)
                .university(university)
                .semester(request.semester())
                .result(request.result())
                .languageTestType(request.languageTestType())
                .languageScore(request.languageScore())
                .gpa(profile.getGpa())
                .majorType(user.getMajorType())
                .build();
        try {
            // 동시 요청은 유니크 제약(user_id, university_id, semester, result) 위반으로 여기서 드러난다
            report = admissionReportRepository.saveAndFlush(report);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.ADMISSION_REPORT_DUPLICATED);
        }
        return AdmissionReportResponse.from(report);
    }

    // ===== 수정 =====

    @Transactional
    public AdmissionReportResponse update(Long userId, Long reportId, AdmissionReportRequest request) {
        AdmissionReport report = findOwnedReport(userId, reportId,
                ErrorCode.ADMISSION_REPORT_FORBIDDEN.getMessage());
        University university = findUniversity(request.universityId());
        // 자기 자신은 제외하고, 내 다른 건과 같은 학교·학기·결과인지 확인
        if (admissionReportRepository.existsByUserIdAndUniversityIdAndSemesterAndResultAndIdNot(
                userId, university.getId(), request.semester(), request.result(), reportId)) {
            throw new CustomException(ErrorCode.ADMISSION_REPORT_DUPLICATED);
        }

        // gpa·majorType은 등록 시점 값을 그대로 유지한다
        report.update(university, request.semester(), request.result(),
                request.languageTestType(), request.languageScore());
        try {
            admissionReportRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.ADMISSION_REPORT_DUPLICATED);
        }
        return AdmissionReportResponse.from(report);
    }

    // ===== 삭제 =====

    @Transactional
    public void delete(Long userId, Long reportId) {
        AdmissionReport report = findOwnedReport(userId, reportId, DELETE_FORBIDDEN_MESSAGE);
        admissionReportRepository.delete(report);
    }

    // ===== 공통 =====

    private University findUniversity(Long universityId) {
        return universityRepository.findById(universityId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNIVERSITY_NOT_FOUND, UNIVERSITY_NOT_FOUND_MESSAGE));
    }

    /** 존재(404) → 소유자(403) 순서로 확인 */
    private AdmissionReport findOwnedReport(Long userId, Long reportId, String forbiddenMessage) {
        AdmissionReport report = admissionReportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.ADMISSION_REPORT_NOT_FOUND));
        if (!report.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.ADMISSION_REPORT_FORBIDDEN, forbiddenMessage);
        }
        return report;
    }
}