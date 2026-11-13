package com.likelion14th._Team.domain.user.service;

import com.likelion14th._Team.domain.admission.repository.AdmissionReportRepository;
import com.likelion14th._Team.domain.bookmark.repository.BookmarkRepository;
import com.likelion14th._Team.domain.returnreport.entity.ReturnReport;
import com.likelion14th._Team.domain.returnreport.repository.ReturnReportRepository;
import com.likelion14th._Team.domain.returnreport.service.ReturnReportService;
import com.likelion14th._Team.domain.studyplan.repository.StudyPlanItemRepository;
import com.likelion14th._Team.domain.studyplan.repository.StudyPlanRepository;
import com.likelion14th._Team.domain.user.entity.User;
import com.likelion14th._Team.domain.user.repository.ConsentRepository;
import com.likelion14th._Team.domain.user.repository.EmailVerificationRepository;
import com.likelion14th._Team.domain.user.repository.UserLanguageScoreRepository;
import com.likelion14th._Team.domain.user.repository.UserProfileRepository;
import com.likelion14th._Team.domain.user.repository.UserRepository;
import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 탈퇴. 계정과 관련 데이터를 하드 삭제한다.
 * DB의 ON DELETE에 의존하지 않고 FK 순서에 맞춰 코드에서 직접 지운다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserLanguageScoreRepository userLanguageScoreRepository;
    private final ConsentRepository consentRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final StudyPlanItemRepository studyPlanItemRepository;
    private final StudyPlanRepository studyPlanRepository;
    private final ReturnReportRepository returnReportRepository;
    private final ReturnReportService returnReportService;
    private final AdmissionReportRepository admissionReportRepository;
    private final BookmarkRepository bookmarkRepository;

    @Transactional
    public void withdraw(Long userId) {
        try {
            doWithdraw(userId);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("회원 탈퇴 처리 중 예외", e);
            throw new CustomException(ErrorCode.INTERNAL_ERROR, "회원 탈퇴 중 오류가 발생했습니다");
        }
    }

    private void doWithdraw(Long userId) {
        // 1. 회원 행을 잠가 같은 회원의 동시 요청을 막는다. 토큰은 유효한데 회원이 없으면 401
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));
        String schoolEmail = user.getSchoolEmail();

        // 2. 내 수강계획: study_plan_item → study_plan
        studyPlanItemRepository.deleteAllByStudyPlanUserId(userId);
        studyPlanRepository.deleteAllByUserId(userId);

        // 3. 내 귀국보고서: course_case → (타 사용자 수강계획 항목의 source_course_id NULL) → 과목 → 보고서
        for (ReturnReport report : returnReportRepository.findAllByUserIdWithUniversity(userId)) {
            returnReportService.deleteReport(report);
        }

        // 4. 나머지 내 데이터
        admissionReportRepository.deleteAllByUserId(userId);
        bookmarkRepository.deleteAllByUserId(userId);
        consentRepository.deleteAllByUserId(userId);
        userLanguageScoreRepository.deleteAllByUserId(userId);
        userProfileRepository.deleteByUserId(userId);

        // 5. 계정, 남아 있는 메일 인증 기록
        userRepository.delete(user);
        emailVerificationRepository.findBySchoolEmail(schoolEmail)
                .ifPresent(emailVerificationRepository::delete);

        // DB 예외를 이 트랜잭션 안(try/catch)에서 드러내기 위해 미리 반영
        userRepository.flush();
    }
}