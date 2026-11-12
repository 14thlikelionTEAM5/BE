package com.likelion14th._Team.domain.returnreport.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.likelion14th._Team.domain.returnreport.dto.ReturnReportCost;
import com.likelion14th._Team.domain.returnreport.dto.ReturnReportCourseRequest;
import com.likelion14th._Team.domain.returnreport.dto.ReturnReportSaveRequest;
import com.likelion14th._Team.domain.returnreport.dto.ReturnReportSaveResponse;
import com.likelion14th._Team.domain.returnreport.dto.ReturnReportUpdateRequest;
import com.likelion14th._Team.domain.returnreport.dto.ReturnReportVisa;
import com.likelion14th._Team.domain.returnreport.entity.ReturnReport;
import com.likelion14th._Team.domain.returnreport.entity.ReturnReportCourse;
import com.likelion14th._Team.domain.coursecase.repository.CourseCaseRepository;
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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

class ReturnReportServiceTests {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long REPORT_ID = 5L;
    private static final Long UNIVERSITY_ID = 12L;

    private final ReturnReportRepository reportRepository = mock(ReturnReportRepository.class);
    private final ReturnReportCourseRepository courseRepository = mock(ReturnReportCourseRepository.class);
    private final UniversityRepository universityRepository = mock(UniversityRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final StudyPlanItemRepository studyPlanItemRepository = mock(StudyPlanItemRepository.class);
    private final CourseCaseRepository courseCaseRepository = mock(CourseCaseRepository.class);
    private final ReturnReportService service = new ReturnReportService(reportRepository, courseRepository,
            universityRepository, userRepository, studyPlanItemRepository, courseCaseRepository);

    private final User user = mock(User.class);
    private final University university = mock(University.class);
    private final AtomicLong nextCourseId = new AtomicLong(100);

    @BeforeEach
    void setUp() {
        when(user.getId()).thenReturn(USER_ID);
        when(university.getId()).thenReturn(UNIVERSITY_ID);
        when(courseRepository.save(any(ReturnReportCourse.class))).thenAnswer(invocation -> {
            ReturnReportCourse course = invocation.getArgument(0);
            ReflectionTestUtils.setField(course, "id", nextCourseId.incrementAndGet());
            return course;
        });
    }

    private static ReturnReportCourseRequest course(Long courseId, String nameEn) {
        return new ReturnReportCourseRequest(courseId, "CODE", nameEn, new BigDecimal("4.0"), new BigDecimal("4"));
    }

    private static ReturnReportSaveRequest saveRequest(List<ReturnReportCourseRequest> courses) {
        return new ReturnReportSaveRequest(UNIVERSITY_ID, "2026-2",
                new ReturnReportCost("왕복 약 90만원", null, "월 150만원", null), "DORM",
                new ReturnReportVisa("약 2주", "Student Pass"), "삼성화재", courses, "요약");
    }

    private ReturnReport existingReport(User owner) {
        ReturnReport report = ReturnReport.builder().user(owner).university(university).semester("2026-2")
                .costFlight("old").housingType("SELF").build();
        ReflectionTestUtils.setField(report, "id", REPORT_ID);
        return report;
    }

    private ReturnReportCourse existingCourse(ReturnReport report, long id, String nameEn) {
        ReturnReportCourse course = ReturnReportCourse.builder().returnReport(report).courseNameEn(nameEn)
                .foreignCredit(new BigDecimal("3.0")).build();
        ReflectionTestUtils.setField(course, "id", id);
        return course;
    }

    // ===== 저장 =====

    @Test
    void saveStoresReportAndCourses() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(universityRepository.findById(UNIVERSITY_ID)).thenReturn(Optional.of(university));
        when(reportRepository.saveAndFlush(any(ReturnReport.class))).thenAnswer(invocation -> {
            ReturnReport report = invocation.getArgument(0);
            ReflectionTestUtils.setField(report, "id", REPORT_ID);
            return report;
        });

        ReturnReportSaveResponse response = service.save(USER_ID,
                saveRequest(List.of(course(null, "Principles of Marketing"), course(null, "Corporate Finance"))));

        assertEquals(REPORT_ID, response.returnReportId());
        assertEquals(UNIVERSITY_ID, response.universityId());
        assertEquals("2026-2", response.semester());
        assertEquals(List.of(101L, 102L), response.courses().stream().map(c -> c.courseId()).toList());
        assertEquals("Principles of Marketing", response.courses().getFirst().courseNameEn());

        ArgumentCaptor<ReturnReport> saved = ArgumentCaptor.forClass(ReturnReport.class);
        verify(reportRepository).saveAndFlush(saved.capture());
        assertEquals("왕복 약 90만원", saved.getValue().getCostFlight());
        assertNull(saved.getValue().getCostInsurance());
        assertEquals("월 150만원", saved.getValue().getCostLivingMonthly());
        assertEquals("DORM", saved.getValue().getHousingType());
        assertEquals("Student Pass", saved.getValue().getVisaType());
        assertEquals("삼성화재", saved.getValue().getInsuranceProvider());
        assertEquals("요약", saved.getValue().getReviewSummary());
    }

    @Test
    void saveWithNullCostAndVisaStoresNulls() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(universityRepository.findById(UNIVERSITY_ID)).thenReturn(Optional.of(university));
        when(reportRepository.saveAndFlush(any(ReturnReport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.save(USER_ID, new ReturnReportSaveRequest(UNIVERSITY_ID, "2026-2", null, null, null, null,
                List.of(), null));

        ArgumentCaptor<ReturnReport> saved = ArgumentCaptor.forClass(ReturnReport.class);
        verify(reportRepository).saveAndFlush(saved.capture());
        assertNull(saved.getValue().getCostFlight());
        assertNull(saved.getValue().getVisaDuration());
        verify(courseRepository, never()).save(any());
    }

    @Test
    void saveRejectsMissingUserUniversityAndDuplicate() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
        assertEquals(ErrorCode.UNAUTHORIZED,
                assertThrows(CustomException.class, () -> service.save(USER_ID, saveRequest(List.of()))).getErrorCode());

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(universityRepository.findById(UNIVERSITY_ID)).thenReturn(Optional.empty());
        CustomException notFound = assertThrows(CustomException.class,
                () -> service.save(USER_ID, saveRequest(List.of())));
        assertEquals(ErrorCode.UNIVERSITY_NOT_FOUND, notFound.getErrorCode());
        assertEquals("존재하지 않는 학교입니다", notFound.getMessage());

        when(universityRepository.findById(UNIVERSITY_ID)).thenReturn(Optional.of(university));
        when(reportRepository.existsByUserIdAndSemester(USER_ID, "2026-2")).thenReturn(true);
        CustomException duplicated = assertThrows(CustomException.class,
                () -> service.save(USER_ID, saveRequest(List.of())));
        assertEquals(ErrorCode.RETURN_REPORT_DUPLICATED, duplicated.getErrorCode());
        assertEquals("이미 해당 학기의 귀국보고서를 등록했습니다", duplicated.getMessage());
        verify(reportRepository, never()).saveAndFlush(any());
    }

    @Test
    void concurrentDuplicateSaveBecomes409() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(universityRepository.findById(UNIVERSITY_ID)).thenReturn(Optional.of(university));
        when(reportRepository.saveAndFlush(any(ReturnReport.class)))
                .thenThrow(new DataIntegrityViolationException("uk_return_report_user_semester"));

        CustomException e = assertThrows(CustomException.class,
                () -> service.save(USER_ID, saveRequest(List.of(course(null, "A")))));

        assertEquals(ErrorCode.RETURN_REPORT_DUPLICATED, e.getErrorCode());
        verify(courseRepository, never()).save(any());
    }

    // ===== 수정 =====

    @Test
    void updateAddsUpdatesAndRemovesCourses() {
        ReturnReport report = existingReport(user);
        ReturnReportCourse keep = existingCourse(report, 21L, "Old Name");
        ReturnReportCourse remove = existingCourse(report, 22L, "To Remove");
        when(reportRepository.findById(REPORT_ID)).thenReturn(Optional.of(report));
        when(courseRepository.findByReturnReportIdOrderByIdAsc(REPORT_ID))
                .thenReturn(new ArrayList<>(List.of(keep, remove)));

        ReturnReportSaveResponse response = service.update(USER_ID, REPORT_ID, new ReturnReportUpdateRequest(
                null, "DORM", null, null,
                List.of(course(null, "New Course"), course(21L, "Principles of Marketing")), "새 요약"));

        verify(studyPlanItemRepository).clearSourceCourse(List.of(22L));
        verify(courseRepository).deleteAll(List.of(remove));
        assertEquals("Principles of Marketing", keep.getCourseNameEn());
        assertEquals(new BigDecimal("4.0"), keep.getForeignCredit());
        assertEquals(4, keep.getSatisfaction());
        assertEquals(List.of(21L, 101L), response.courses().stream().map(c -> c.courseId()).toList());
        assertEquals("2026-2", response.semester());
        assertNull(report.getCostFlight());
        assertEquals("DORM", report.getHousingType());
        assertEquals("새 요약", report.getReviewSummary());
    }

    @Test
    void updateWithoutRemovedRowsDoesNotTouchStudyPlan() {
        ReturnReport report = existingReport(user);
        when(reportRepository.findById(REPORT_ID)).thenReturn(Optional.of(report));
        when(courseRepository.findByReturnReportIdOrderByIdAsc(REPORT_ID))
                .thenReturn(List.of(existingCourse(report, 21L, "A")));

        service.update(USER_ID, REPORT_ID,
                new ReturnReportUpdateRequest(null, null, null, null, List.of(course(21L, "A")), null));

        verify(studyPlanItemRepository, never()).clearSourceCourse(anyCollection());
        verify(courseRepository, never()).deleteAll(any());
    }

    @Test
    void updateChecksExistenceThenOwnership() {
        when(reportRepository.findById(REPORT_ID)).thenReturn(Optional.empty());
        CustomException notFound = assertThrows(CustomException.class, () -> service.update(USER_ID, REPORT_ID,
                new ReturnReportUpdateRequest(null, null, null, null, List.of(), null)));
        assertEquals(ErrorCode.RETURN_REPORT_NOT_FOUND, notFound.getErrorCode());
        assertEquals("존재하지 않는 귀국보고서입니다", notFound.getMessage());

        User other = mock(User.class);
        when(other.getId()).thenReturn(OTHER_USER_ID);
        when(reportRepository.findById(REPORT_ID)).thenReturn(Optional.of(existingReport(other)));
        CustomException forbidden = assertThrows(CustomException.class, () -> service.update(USER_ID, REPORT_ID,
                new ReturnReportUpdateRequest(null, null, null, null, List.of(), null)));
        assertEquals(ErrorCode.RETURN_REPORT_FORBIDDEN, forbidden.getErrorCode());
        assertEquals("본인의 귀국보고서만 수정할 수 있습니다", forbidden.getMessage());
    }

    @Test
    void updateRejectsForeignOrDuplicatedCourseIdsWithFieldErrors() {
        ReturnReport report = existingReport(user);
        when(reportRepository.findById(REPORT_ID)).thenReturn(Optional.of(report));
        when(courseRepository.findByReturnReportIdOrderByIdAsc(REPORT_ID))
                .thenReturn(List.of(existingCourse(report, 21L, "A")));

        CustomException e = assertThrows(CustomException.class, () -> service.update(USER_ID, REPORT_ID,
                new ReturnReportUpdateRequest(null, null, null, null,
                        List.of(course(21L, "A"), course(999L, "B"), course(null, "C"), course(21L, "D")), null)));

        assertEquals(ErrorCode.INVALID_INPUT, e.getErrorCode());
        assertEquals("수정할 수 없는 교과목이 포함되어 있습니다", e.getMessage());
        assertEquals(List.of(
                new FieldErrorDetail("courses[1].courseId", "이 귀국보고서의 교과목이 아닙니다"),
                new FieldErrorDetail("courses[3].courseId", "같은 교과목이 중복되었습니다")), e.getErrors());
        verify(studyPlanItemRepository, never()).clearSourceCourse(anyCollection());
        verify(courseRepository, never()).save(any());
    }

    // ===== 삭제 =====

    @Test
    void deleteClearsStudyPlanLinksThenCoursesThenReport() {
        ReturnReport report = existingReport(user);
        when(reportRepository.findById(REPORT_ID)).thenReturn(Optional.of(report));
        when(courseRepository.findIdsByReturnReportId(REPORT_ID)).thenReturn(List.of(21L, 22L));

        service.delete(USER_ID, REPORT_ID);

        InOrder order = inOrder(studyPlanItemRepository, courseRepository, reportRepository);
        order.verify(studyPlanItemRepository).clearSourceCourse(List.of(21L, 22L));
        order.verify(courseRepository).deleteAllByReturnReportId(REPORT_ID);
        order.verify(reportRepository).delete(report);
    }

    @Test
    void deleteWithoutCoursesOnlyDeletesReport() {
        ReturnReport report = existingReport(user);
        when(reportRepository.findById(REPORT_ID)).thenReturn(Optional.of(report));
        when(courseRepository.findIdsByReturnReportId(REPORT_ID)).thenReturn(List.of());

        service.delete(USER_ID, REPORT_ID);

        verify(studyPlanItemRepository, never()).clearSourceCourse(anyCollection());
        verify(reportRepository).delete(report);
    }

    @Test
    void deleteChecksExistenceThenOwnershipWithDeleteMessage() {
        when(reportRepository.findById(REPORT_ID)).thenReturn(Optional.empty());
        assertEquals(ErrorCode.RETURN_REPORT_NOT_FOUND,
                assertThrows(CustomException.class, () -> service.delete(USER_ID, REPORT_ID)).getErrorCode());

        User other = mock(User.class);
        when(other.getId()).thenReturn(OTHER_USER_ID);
        when(reportRepository.findById(REPORT_ID)).thenReturn(Optional.of(existingReport(other)));
        CustomException forbidden = assertThrows(CustomException.class, () -> service.delete(USER_ID, REPORT_ID));
        assertEquals(ErrorCode.RETURN_REPORT_FORBIDDEN, forbidden.getErrorCode());
        assertEquals("본인의 귀국보고서만 삭제할 수 있습니다", forbidden.getMessage());
        verify(reportRepository, never()).delete(any());
    }
}
