package com.likelion14th._Team.domain.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.likelion14th._Team.domain.admission.repository.AdmissionReportRepository;
import com.likelion14th._Team.domain.bookmark.repository.BookmarkRepository;
import com.likelion14th._Team.domain.returnreport.entity.ReturnReport;
import com.likelion14th._Team.domain.returnreport.repository.ReturnReportRepository;
import com.likelion14th._Team.domain.returnreport.service.ReturnReportService;
import com.likelion14th._Team.domain.studyplan.repository.StudyPlanItemRepository;
import com.likelion14th._Team.domain.studyplan.repository.StudyPlanRepository;
import com.likelion14th._Team.domain.user.entity.EmailVerification;
import com.likelion14th._Team.domain.user.entity.User;
import com.likelion14th._Team.domain.user.repository.ConsentRepository;
import com.likelion14th._Team.domain.user.repository.EmailVerificationRepository;
import com.likelion14th._Team.domain.user.repository.UserLanguageScoreRepository;
import com.likelion14th._Team.domain.user.repository.UserProfileRepository;
import com.likelion14th._Team.domain.user.repository.UserRepository;
import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.dao.DataIntegrityViolationException;

class WithdrawServiceTests {

    private static final Long USER_ID = 1L;
    private static final String EMAIL = "chaewon@hufs.ac.kr";

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserProfileRepository userProfileRepository = mock(UserProfileRepository.class);
    private final UserLanguageScoreRepository scoreRepository = mock(UserLanguageScoreRepository.class);
    private final ConsentRepository consentRepository = mock(ConsentRepository.class);
    private final EmailVerificationRepository emailVerificationRepository = mock(EmailVerificationRepository.class);
    private final StudyPlanItemRepository studyPlanItemRepository = mock(StudyPlanItemRepository.class);
    private final StudyPlanRepository studyPlanRepository = mock(StudyPlanRepository.class);
    private final ReturnReportRepository returnReportRepository = mock(ReturnReportRepository.class);
    private final ReturnReportService returnReportService = mock(ReturnReportService.class);
    private final AdmissionReportRepository admissionReportRepository = mock(AdmissionReportRepository.class);
    private final BookmarkRepository bookmarkRepository = mock(BookmarkRepository.class);

    private final WithdrawService service = new WithdrawService(
            userRepository, userProfileRepository, scoreRepository, consentRepository,
            emailVerificationRepository, studyPlanItemRepository, studyPlanRepository,
            returnReportRepository, returnReportService, admissionReportRepository, bookmarkRepository);

    private final User user = mock(User.class);
    private final ReturnReport report1 = mock(ReturnReport.class);
    private final ReturnReport report2 = mock(ReturnReport.class);
    private final EmailVerification verification = mock(EmailVerification.class);

    @BeforeEach
    void setUp() {
        when(user.getSchoolEmail()).thenReturn(EMAIL);
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        when(returnReportRepository.findAllByUserIdWithUniversity(USER_ID)).thenReturn(List.of(report1, report2));
        when(emailVerificationRepository.findBySchoolEmail(EMAIL)).thenReturn(Optional.of(verification));
    }

    @Test
    void deletesEverythingInSpecOrder() {
        service.withdraw(USER_ID);

        InOrder order = inOrder(userRepository, studyPlanItemRepository, studyPlanRepository,
                returnReportService, admissionReportRepository, bookmarkRepository, consentRepository,
                scoreRepository, userProfileRepository, emailVerificationRepository);
        order.verify(userRepository).findByIdForUpdate(USER_ID);
        order.verify(studyPlanItemRepository).deleteAllByStudyPlanUserId(USER_ID);
        order.verify(studyPlanRepository).deleteAllByUserId(USER_ID);
        order.verify(returnReportService).deleteReport(report1);
        order.verify(returnReportService).deleteReport(report2);
        order.verify(admissionReportRepository).deleteAllByUserId(USER_ID);
        order.verify(bookmarkRepository).deleteAllByUserId(USER_ID);
        order.verify(consentRepository).deleteAllByUserId(USER_ID);
        order.verify(scoreRepository).deleteAllByUserId(USER_ID);
        order.verify(userProfileRepository).deleteByUserId(USER_ID);
        order.verify(userRepository).delete(user);
        order.verify(emailVerificationRepository).delete(verification);
        order.verify(userRepository).flush();
    }

    @Test
    void withoutReturnReportsStillDeletesTheRest() {
        when(returnReportRepository.findAllByUserIdWithUniversity(USER_ID)).thenReturn(List.of());

        service.withdraw(USER_ID);

        verifyNoInteractions(returnReportService);
        verify(admissionReportRepository).deleteAllByUserId(USER_ID);
        verify(userRepository).delete(user);
    }

    @Test
    void withoutEmailVerificationSkipsItsDeletion() {
        when(emailVerificationRepository.findBySchoolEmail(EMAIL)).thenReturn(Optional.empty());

        service.withdraw(USER_ID);

        verify(emailVerificationRepository, never()).delete(any(EmailVerification.class));
        verify(userRepository).delete(user);
    }

    @Test
    void deletedUserIs401AndNothingIsDeleted() {
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.empty());

        CustomException e = assertThrows(CustomException.class, () -> service.withdraw(USER_ID));

        assertEquals(ErrorCode.UNAUTHORIZED, e.getErrorCode());
        verifyNoInteractions(studyPlanItemRepository, studyPlanRepository, returnReportService,
                admissionReportRepository, bookmarkRepository, consentRepository, scoreRepository,
                userProfileRepository, emailVerificationRepository);
        verify(userRepository, never()).delete(any());
    }

    @Test
    void failureWhileDeletingIs500AndAccountIsNotDeleted() {
        doThrow(new IllegalStateException("db error")).when(studyPlanRepository).deleteAllByUserId(USER_ID);

        CustomException e = assertThrows(CustomException.class, () -> service.withdraw(USER_ID));

        assertEquals(ErrorCode.INTERNAL_ERROR, e.getErrorCode());
        assertEquals("회원 탈퇴 중 오류가 발생했습니다", e.getMessage());
        verify(userRepository, never()).delete(any());
    }

    @Test
    void databaseErrorOnFlushIs500() {
        doThrow(new DataIntegrityViolationException("fk")).when(userRepository).flush();

        CustomException e = assertThrows(CustomException.class, () -> service.withdraw(USER_ID));

        assertEquals(ErrorCode.INTERNAL_ERROR, e.getErrorCode());
        assertEquals("회원 탈퇴 중 오류가 발생했습니다", e.getMessage());
    }
}