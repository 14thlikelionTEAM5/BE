package com.likelion14th._Team.domain.experience.service;

import com.likelion14th._Team.domain.admission.repository.AdmissionReportRepository;
import com.likelion14th._Team.domain.experience.dto.DuplicateCheckResponse;
import com.likelion14th._Team.domain.returnreport.repository.ReturnReportRepository;
import com.likelion14th._Team.global.enums.AdmissionResult;
import com.likelion14th._Team.global.enums.DuplicateReason;
import com.likelion14th._Team.global.enums.ExperienceType;
import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import com.likelion14th._Team.global.response.FieldErrorDetail;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExperienceDuplicateService {

    static final String TYPE_FIELD = "type";
    static final String UNIVERSITY_ID_FIELD = "universityId";
    static final String SEMESTER_FIELD = "semester";
    private static final Pattern SEMESTER_PATTERN = Pattern.compile("^\\d{4}-[12]$");

    private final ReturnReportRepository returnReportRepository;
    private final AdmissionReportRepository admissionReportRepository;

    /** 새로 등록하기 전에 선택한 유형·학교·학기로 등록할 수 있는지 확인 */
    public DuplicateCheckResponse check(Long userId, String rawType, Long universityId, String rawSemester) {
        List<FieldErrorDetail> errors = new ArrayList<>();
        ExperienceType type = parseType(rawType, errors);
        if (universityId == null) {
            errors.add(new FieldErrorDetail(UNIVERSITY_ID_FIELD, "학교를 선택해 주세요"));
        }
        String semester = rawSemester == null ? null : rawSemester.trim();
        if (semester == null || semester.isEmpty()) {
            errors.add(new FieldErrorDetail(SEMESTER_FIELD, "학기를 입력해 주세요"));
        } else if (!SEMESTER_PATTERN.matcher(semester).matches()) {
            errors.add(new FieldErrorDetail(SEMESTER_FIELD, "학기는 YYYY-1 또는 YYYY-2 형식이어야 합니다"));
        }
        if (!errors.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT, ErrorCode.INVALID_INPUT.getMessage(), errors);
        }

        try {
            return switch (type) {
                case RETURN_REPORT -> returnReportRepository.existsByUserIdAndSemester(userId, semester)
                        ? DuplicateCheckResponse.unavailable(DuplicateReason.ALREADY_REGISTERED)
                        : DuplicateCheckResponse.ok();
                case PASS -> checkAdmission(userId, universityId, semester, AdmissionResult.PASS);
                case FAIL -> checkAdmission(userId, universityId, semester, AdmissionResult.FAIL);
                case COURSE_CASE -> returnReportRepository
                        .existsByUserIdAndUniversityIdAndSemester(userId, universityId, semester)
                        ? DuplicateCheckResponse.ok()
                        : DuplicateCheckResponse.unavailable(DuplicateReason.REPORT_REQUIRED);
            };
        } catch (RuntimeException e) {
            log.error("등록 중복 확인 실패", e);
            throw new CustomException(ErrorCode.INTERNAL_ERROR, "등록 확인 중 오류가 발생했습니다.");
        }
    }

    private DuplicateCheckResponse checkAdmission(Long userId, Long universityId, String semester,
                                                  AdmissionResult result) {
        return admissionReportRepository.existsByUserIdAndUniversityIdAndSemesterAndResult(
                userId, universityId, semester, result)
                ? DuplicateCheckResponse.unavailable(DuplicateReason.ALREADY_REGISTERED)
                : DuplicateCheckResponse.ok();
    }

    private ExperienceType parseType(String raw, List<FieldErrorDetail> errors) {
        if (raw == null || raw.isBlank()) {
            errors.add(new FieldErrorDetail(TYPE_FIELD, "등록 유형을 입력해 주세요"));
            return null;
        }
        for (ExperienceType type : ExperienceType.values()) {
            if (type.name().equals(raw)) {
                return type;
            }
        }
        errors.add(new FieldErrorDetail(TYPE_FIELD,
                "RETURN_REPORT, PASS, FAIL, COURSE_CASE만 사용할 수 있습니다"));
        return null;
    }
}