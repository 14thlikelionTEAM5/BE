package com.likelion14th._Team.domain.experience.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.likelion14th._Team.domain.admission.repository.AdmissionReportRepository;
import com.likelion14th._Team.domain.experience.dto.DuplicateCheckResponse;
import com.likelion14th._Team.domain.returnreport.repository.ReturnReportCourseRepository;
import com.likelion14th._Team.domain.returnreport.repository.ReturnReportRepository;
import com.likelion14th._Team.global.enums.AdmissionResult;
import com.likelion14th._Team.global.enums.DuplicateReason;
import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import com.likelion14th._Team.global.response.FieldErrorDetail;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExperienceDuplicateServiceTests {

    private static final Long USER_ID = 1L;
    private static final Long UNIVERSITY_ID = 12L;
    private static final String SEMESTER = "2027-1";

    private final ReturnReportRepository returnReportRepository = mock(ReturnReportRepository.class);
    private final ReturnReportCourseRepository returnReportCourseRepository =
            mock(ReturnReportCourseRepository.class);
    private final AdmissionReportRepository admissionReportRepository = mock(AdmissionReportRepository.class);
    private final ExperienceDuplicateService service = new ExperienceDuplicateService(
            returnReportRepository, returnReportCourseRepository, admissionReportRepository);

    @Test
    void returnReportIsUnavailableWhenSemesterAlreadyHasOne() {
        when(returnReportRepository.existsByUserIdAndSemester(USER_ID, SEMESTER)).thenReturn(true);

        DuplicateCheckResponse result = service.check(USER_ID, "RETURN_REPORT", UNIVERSITY_ID, SEMESTER);

        assertFalse(result.available());
        assertEquals(DuplicateReason.ALREADY_REGISTERED, result.reason());
    }

    @Test
    void returnReportIsAvailableWhenSemesterHasNone() {
        when(returnReportRepository.existsByUserIdAndSemester(USER_ID, SEMESTER)).thenReturn(false);

        DuplicateCheckResponse result = service.check(USER_ID, "RETURN_REPORT", UNIVERSITY_ID, SEMESTER);

        assertTrue(result.available());
        assertNull(result.reason());
    }

    @Test
    void passAndFailAreCheckedSeparately() {
        when(admissionReportRepository.existsByUserIdAndUniversityIdAndSemesterAndResult(
                USER_ID, UNIVERSITY_ID, SEMESTER, AdmissionResult.PASS)).thenReturn(true);
        when(admissionReportRepository.existsByUserIdAndUniversityIdAndSemesterAndResult(
                USER_ID, UNIVERSITY_ID, SEMESTER, AdmissionResult.FAIL)).thenReturn(false);

        DuplicateCheckResponse pass = service.check(USER_ID, "PASS", UNIVERSITY_ID, SEMESTER);
        DuplicateCheckResponse fail = service.check(USER_ID, "FAIL", UNIVERSITY_ID, SEMESTER);

        assertFalse(pass.available());
        assertEquals(DuplicateReason.ALREADY_REGISTERED, pass.reason());
        assertTrue(fail.available());
        assertNull(fail.reason());
    }

    @Test
    void courseCaseIsAvailableWhenReportHasCourses() {
        when(returnReportCourseRepository.existsByUserIdAndUniversityIdAndSemester(
                USER_ID, UNIVERSITY_ID, SEMESTER)).thenReturn(true);

        DuplicateCheckResponse result = service.check(USER_ID, "COURSE_CASE", UNIVERSITY_ID, SEMESTER);

        assertTrue(result.available());
        assertNull(result.reason());
    }

    @Test
    void courseCaseRequiresReportWithCourses() {
        // 귀국보고서가 없거나, 있어도 수강 교과목이 하나도 없으면 같은 결과
        when(returnReportCourseRepository.existsByUserIdAndUniversityIdAndSemester(
                USER_ID, UNIVERSITY_ID, SEMESTER)).thenReturn(false);

        DuplicateCheckResponse result = service.check(USER_ID, "COURSE_CASE", UNIVERSITY_ID, SEMESTER);

        assertFalse(result.available());
        assertEquals(DuplicateReason.REPORT_REQUIRED, result.reason());
        verifyNoInteractions(returnReportRepository);
    }

    @Test
    void reportsAllMissingFieldsTogether() {
        CustomException e = assertThrows(CustomException.class, () -> service.check(USER_ID, null, null, null));

        assertEquals(ErrorCode.INVALID_INPUT, e.getErrorCode());
        assertEquals(List.of("type", "universityId", "semester"),
                e.getErrors().stream().map(FieldErrorDetail::field).toList());
        verifyNoInteractions(returnReportRepository, returnReportCourseRepository, admissionReportRepository);
    }

    @Test
    void rejectsUnknownTypeAndMalformedSemester() {
        for (String type : new String[]{"ABC", "pass", "PASS ", ""}) {
            CustomException e = assertThrows(CustomException.class,
                    () -> service.check(USER_ID, type, UNIVERSITY_ID, SEMESTER));
            assertEquals(List.of("type"), e.getErrors().stream().map(FieldErrorDetail::field).toList());
        }
        for (String semester : new String[]{"2027-3", "27-1", "2027-0", "2027/1", " "}) {
            CustomException e = assertThrows(CustomException.class,
                    () -> service.check(USER_ID, "PASS", UNIVERSITY_ID, semester));
            assertEquals(List.of("semester"), e.getErrors().stream().map(FieldErrorDetail::field).toList());
        }
    }

    @Test
    void wrapsRepositoryFailureAs500() {
        when(returnReportCourseRepository.existsByUserIdAndUniversityIdAndSemester(
                USER_ID, UNIVERSITY_ID, SEMESTER)).thenThrow(new IllegalStateException("db down"));

        CustomException e = assertThrows(CustomException.class,
                () -> service.check(USER_ID, "COURSE_CASE", UNIVERSITY_ID, SEMESTER));

        assertEquals(ErrorCode.INTERNAL_ERROR, e.getErrorCode());
        assertEquals("등록 확인 중 오류가 발생했습니다.", e.getMessage());
    }
}