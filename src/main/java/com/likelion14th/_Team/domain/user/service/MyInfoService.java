package com.likelion14th._Team.domain.user.service;

import com.likelion14th._Team.domain.user.dto.MyInfoResponse;
import com.likelion14th._Team.domain.user.dto.MyInfoUpdateRequest;
import com.likelion14th._Team.domain.user.dto.MyInfoUpdateResponse;
import com.likelion14th._Team.domain.auth.dto.SignupRequest.LanguageScoreRequest;
import com.likelion14th._Team.domain.user.entity.User;
import com.likelion14th._Team.domain.user.entity.UserLanguageScore;
import com.likelion14th._Team.domain.user.entity.UserProfile;
import com.likelion14th._Team.domain.user.repository.ConsentRepository;
import com.likelion14th._Team.domain.user.repository.UserLanguageScoreRepository;
import com.likelion14th._Team.domain.user.repository.UserProfileRepository;
import com.likelion14th._Team.domain.user.repository.UserRepository;
import com.likelion14th._Team.global.enums.ConsentType;
import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import com.likelion14th._Team.global.logging.ExceptionLogSanitizer;
import java.math.RoundingMode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MyInfoService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserLanguageScoreRepository userLanguageScoreRepository;
    private final ConsentRepository consentRepository;

    // ===== 내 정보 조회 =====

    @Transactional(readOnly = true)
    public MyInfoResponse getMyInfo(Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));
            UserProfile profile = findProfile(userId);
            List<UserLanguageScore> scores = userLanguageScoreRepository.findByUserIdOrderByIdAsc(userId);
            boolean consented = consentRepository.existsByUserIdAndConsentType(userId, ConsentType.EXTERNAL_AI);
            return MyInfoResponse.of(user, profile, scores, consented);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("내 정보 조회 중 예외", ExceptionLogSanitizer.withoutMessages(e));
            throw new CustomException(ErrorCode.INTERNAL_ERROR, "내 정보 조회 중 오류가 발생했습니다");
        }
    }

    // ===== 내 정보 수정 =====

    @Transactional
    public MyInfoUpdateResponse update(Long userId, MyInfoUpdateRequest request) {
        try {
            return doUpdate(userId, request);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("내 정보 수정 중 예외", ExceptionLogSanitizer.withoutMessages(e));
            throw new CustomException(ErrorCode.INTERNAL_ERROR, "내 정보 수정 중 오류가 발생했습니다");
        }
    }

    private MyInfoUpdateResponse doUpdate(Long userId, MyInfoUpdateRequest request) {
        // 같은 회원의 동시 수정을 순서대로 처리
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));
        UserProfile profile = findProfile(userId);

        if (request.nickname() != null && !request.nickname().equals(user.getNickname())) {
            changeNickname(user, request.nickname());
        }
        if (request.majorType() != null) {
            user.changeMajorType(request.majorType());
        }
        if (request.gpa() != null) {
            profile.changeGpa(request.gpa().setScale(2, RoundingMode.HALF_UP));
        }

        List<UserLanguageScore> scores;
        if (request.languageScores() != null) {
            scores = replaceLanguageScores(user, request.languageScores());
        } else {
            scores = userLanguageScoreRepository.findByUserIdOrderByIdAsc(userId);
        }

        return MyInfoUpdateResponse.of(user, profile, scores);
    }

    /** 대소문자 무시 중복 확인 (본인 현재 닉네임은 제외). 동시 요청의 유니크 위반도 409로 변환 */
    private void changeNickname(User user, String nickname) {
        userRepository.findByNicknameIgnoreCase(nickname)
                .filter(owner -> !owner.getId().equals(user.getId()))
                .ifPresent(owner -> {
                    throw new CustomException(ErrorCode.NICKNAME_DUPLICATED);
                });
        user.changeNickname(nickname);
        try {
            userRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.NICKNAME_DUPLICATED);
        }
    }

    /** 기존 행을 모두 지우고 보낸 목록으로 교체. 빈 배열이면 전체 삭제 */
    private List<UserLanguageScore> replaceLanguageScores(User user, List<LanguageScoreRequest> items) {
        userLanguageScoreRepository.deleteAllByUserId(user.getId());
        if (items.isEmpty()) {
            return List.of();
        }
        List<UserLanguageScore> entities = items.stream()
                .map(item -> UserLanguageScore.builder()
                        .user(user)
                        .testType(item.testType())
                        .score(item.score().setScale(1, RoundingMode.HALF_UP))
                        .build())
                .toList();
        return userLanguageScoreRepository.saveAll(entities);
    }

    private UserProfile findProfile(Long userId) {
        return userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("user_profile이 없는 회원입니다. userId=" + userId));
    }
}