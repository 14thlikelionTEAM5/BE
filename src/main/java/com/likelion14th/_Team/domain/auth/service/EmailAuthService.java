package com.likelion14th._Team.domain.auth.service;

import com.likelion14th._Team.domain.auth.dto.EmailSendRequest;
import com.likelion14th._Team.domain.auth.dto.EmailSendResponse;
import com.likelion14th._Team.domain.auth.dto.EmailVerifyRequest;
import com.likelion14th._Team.domain.auth.dto.EmailVerifyResponse;
import com.likelion14th._Team.domain.user.entity.EmailVerification;
import com.likelion14th._Team.domain.user.repository.EmailVerificationRepository;
import com.likelion14th._Team.domain.user.repository.UserRepository;
import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 학교 메일 인증 (발송 / 확인).
 * 메일 발송처럼 오래 걸리는 작업 동안 DB 트랜잭션을 잡고 있지 않도록,
 * 메서드 전체에 @Transactional을 걸지 않고 repository 호출마다 저장한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailAuthService {

    private static final String SCHOOL_EMAIL_DOMAIN = "@hufs.ac.kr";
    private static final Pattern CODE_PATTERN = Pattern.compile("^\\d{6}$");
    private static final long CODE_VALID_MINUTES = 5L;
    private static final long RESEND_COOLDOWN_SECONDS = 60L;
    private static final long VERIFIED_VALID_MINUTES = 30L;
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationMailSender mailSender;

    /** 메일별 코드 입력 실패 횟수 (메모리). 서버 재시작 시 초기화된다 */
    private final Map<String, Integer> failedAttempts = new ConcurrentHashMap<>();

    // ===== 인증 코드 발송 =====

    public EmailSendResponse sendCode(EmailSendRequest request) {
        try {
            return doSendCode(request);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("학교 메일 인증 코드 발송 중 예외", e);
            throw new CustomException(ErrorCode.INTERNAL_ERROR, "메일 발송 중 오류가 발생했습니다");
        }
    }

    private EmailSendResponse doSendCode(EmailSendRequest request) {
        String email = normalize(request.schoolEmail());
        requireSchoolEmail(email);

        // 이미 가입된 메일이면 발송하지 않는다
        if (userRepository.existsBySchoolEmail(email)) {
            throw new CustomException(ErrorCode.EMAIL_DUPLICATED);
        }

        // 직전 발송 후 60초 이내면 거절
        EmailVerification existing = emailVerificationRepository.findBySchoolEmail(email).orElse(null);
        LocalDateTime now = LocalDateTime.now(SEOUL);
        if (existing != null
                && existing.getLastSentAt().plusSeconds(RESEND_COOLDOWN_SECONDS).isAfter(now)) {
            throw new CustomException(ErrorCode.TOO_MANY_REQUESTS);
        }

        String code = generateCode();
        String codeHash = passwordEncoder.encode(code);

        // 메일 발송이 성공한 뒤에만 DB에 저장한다 (실패하면 예외 → 저장 안 됨)
        mailSender.send(email, code);

        LocalDateTime sentAt = LocalDateTime.now(SEOUL).withNano(0);
        LocalDateTime expiresAt = sentAt.plusMinutes(CODE_VALID_MINUTES);
        LocalDateTime resendAvailableAt = sentAt.plusSeconds(RESEND_COOLDOWN_SECONDS);

        if (existing != null) {
            existing.renew(codeHash, expiresAt, sentAt); // 이전 코드 무효화
            emailVerificationRepository.save(existing);
        } else {
            emailVerificationRepository.save(EmailVerification.builder()
                    .schoolEmail(email)
                    .codeHash(codeHash)
                    .expiresAt(expiresAt)
                    .lastSentAt(sentAt)
                    .build());
        }
        failedAttempts.remove(email);

        return new EmailSendResponse(email, expiresAt, resendAvailableAt);
    }

    // ===== 인증 코드 확인 =====

    public EmailVerifyResponse verifyCode(EmailVerifyRequest request) {
        try {
            return doVerifyCode(request);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("학교 메일 인증 확인 중 예외", e);
            throw new CustomException(ErrorCode.INTERNAL_ERROR, "인증 확인 중 오류가 발생했습니다");
        }
    }

    private EmailVerifyResponse doVerifyCode(EmailVerifyRequest request) {
        String email = normalize(request.schoolEmail());
        requireSchoolEmail(email);

        String code = request.code().trim();
        if (!CODE_PATTERN.matcher(code).matches()) {
            throw new CustomException(ErrorCode.INVALID_CODE_FORMAT);
        }

        EmailVerification verification = emailVerificationRepository.findBySchoolEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.CODE_EXPIRED_OR_MISSING));

        LocalDateTime now = LocalDateTime.now(SEOUL).withNano(0);

        // 이미 인증이 끝난 코드이거나 만료된 코드는 사용할 수 없다
        if (verification.getVerifiedAt() != null || !verification.getExpiresAt().isAfter(now)) {
            throw new CustomException(ErrorCode.CODE_EXPIRED_OR_MISSING);
        }

        if (!passwordEncoder.matches(code, verification.getCodeHash())) {
            int failed = failedAttempts.merge(email, 1, Integer::sum);
            if (failed >= MAX_FAILED_ATTEMPTS) {
                // 5번째 실패: 코드를 무효화해서 재발송을 유도한다
                verification.expire(now);
                emailVerificationRepository.save(verification);
                failedAttempts.remove(email);
                throw new CustomException(ErrorCode.CODE_EXPIRED_OR_MISSING);
            }
            throw new CustomException(ErrorCode.CODE_MISMATCH);
        }

        verification.markVerified(now);
        emailVerificationRepository.save(verification);
        failedAttempts.remove(email);

        return new EmailVerifyResponse(email, true, now.plusMinutes(VERIFIED_VALID_MINUTES));
    }

    // ===== 공통 =====

    /** 앞뒤 공백 제거 + 소문자 (회원가입 API와 같은 방식이라 같은 행을 찾는다) */
    private String normalize(String email) {
        return email.trim().toLowerCase();
    }

    private void requireSchoolEmail(String email) {
        if (!email.endsWith(SCHOOL_EMAIL_DOMAIN)) {
            throw new CustomException(ErrorCode.NOT_SCHOOL_EMAIL);
        }
    }

    private String generateCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }
}