package com.likelion14th._Team.domain.user.service;

import com.likelion14th._Team.domain.user.dto.ConsentRequest;
import com.likelion14th._Team.domain.user.dto.ConsentResponse;
import com.likelion14th._Team.domain.user.dto.ConsentResult;
import com.likelion14th._Team.domain.user.entity.Consent;
import com.likelion14th._Team.domain.user.entity.User;
import com.likelion14th._Team.domain.user.repository.ConsentRepository;
import com.likelion14th._Team.domain.user.repository.UserRepository;
import com.likelion14th._Team.global.enums.ConsentType;
import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import com.likelion14th._Team.global.response.FieldErrorDetail;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * 외부 AI 처리 동의 기록.
 * 동시 요청으로 유니크 제약(user_id, consent_type) 위반이 나면 기존 기록을 다시 조회해야 하므로,
 * 메서드 전체를 하나의 트랜잭션으로 묶지 않고 저장(saveAndFlush)만 별도 트랜잭션으로 실행한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsentService {

    static final String CONSENT_TYPE_FIELD = "consentType";
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final ConsentRepository consentRepository;
    private final UserRepository userRepository;

    public ConsentResult record(Long userId, ConsentRequest request) {
        ConsentType consentType = parseConsentType(request);
        try {
            var existing = consentRepository.findByUserIdAndConsentType(userId, consentType);
            if (existing.isPresent()) {
                return new ConsentResult(ConsentResponse.from(existing.get()), false);
            }

            // 토큰은 유효하지만 탈퇴 등으로 회원이 없으면 로그인이 필요한 것으로 본다
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));
            Consent consent = Consent.builder()
                    .user(user)
                    .consentType(consentType)
                    .consentedAt(LocalDateTime.now(SEOUL).withNano(0))
                    .build();
            try {
                return new ConsentResult(ConsentResponse.from(consentRepository.saveAndFlush(consent)), true);
            } catch (DataIntegrityViolationException e) {
                // 동시에 들어온 다른 요청이 먼저 저장한 경우
                return consentRepository.findByUserIdAndConsentType(userId, consentType)
                        .map(saved -> new ConsentResult(ConsentResponse.from(saved), false))
                        .orElseThrow(() -> e);
            }
        } catch (CustomException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("동의 기록 실패", e);
            throw new CustomException(ErrorCode.INTERNAL_ERROR, "동의 기록 중 오류가 발생했습니다.");
        }
    }
    /** 외부 AI로 데이터를 보내는 API가 호출 전에 확인한다. 동의 기록이 없으면 403 */
    public void requireExternalAiConsent(Long userId) {
        if (!consentRepository.existsByUserIdAndConsentType(userId, ConsentType.EXTERNAL_AI)) {
            throw new CustomException(ErrorCode.EXTERNAL_AI_CONSENT_REQUIRED);
        }
    }

    /** 현재는 EXTERNAL_AI만 받는다. 이용약관·개인정보 동의는 회원가입 API에서 기록한다 */
    private ConsentType parseConsentType(ConsentRequest request) {
        String raw = request == null ? null : request.consentType();
        if (raw == null || raw.isBlank()) {
            throw invalid("동의 유형을 입력해 주세요");
        }
        if (!ConsentType.EXTERNAL_AI.name().equals(raw)) {
            throw invalid("EXTERNAL_AI만 사용할 수 있습니다");
        }
        return ConsentType.EXTERNAL_AI;
    }

    private CustomException invalid(String reason) {
        return new CustomException(ErrorCode.INVALID_INPUT, ErrorCode.INVALID_INPUT.getMessage(),
                List.of(new FieldErrorDetail(CONSENT_TYPE_FIELD, reason)));
    }
}
