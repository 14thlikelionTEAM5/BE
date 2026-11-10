package com.likelion14th._Team.domain.auth.service;

import com.likelion14th._Team.domain.auth.dto.LoginRequest;
import com.likelion14th._Team.domain.auth.dto.LoginResponse;
import com.likelion14th._Team.domain.auth.dto.SignupRequest;
import com.likelion14th._Team.domain.auth.dto.SignupResponse;
import com.likelion14th._Team.domain.user.entity.Consent;
import com.likelion14th._Team.domain.user.entity.EmailVerification;
import com.likelion14th._Team.domain.user.entity.User;
import com.likelion14th._Team.domain.user.entity.UserLanguageScore;
import com.likelion14th._Team.domain.user.entity.UserProfile;
import com.likelion14th._Team.domain.user.repository.ConsentRepository;
import com.likelion14th._Team.domain.user.repository.EmailVerificationRepository;
import com.likelion14th._Team.domain.user.repository.UserLanguageScoreRepository;
import com.likelion14th._Team.domain.user.repository.UserProfileRepository;
import com.likelion14th._Team.domain.user.repository.UserRepository;
import com.likelion14th._Team.global.enums.ConsentType;
import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import com.likelion14th._Team.global.security.JwtProvider;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AuthService {

    private static final String SCHOOL_EMAIL_DOMAIN = "@hufs.ac.kr";
    private static final long VERIFICATION_VALID_MINUTES = 30L;
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserLanguageScoreRepository userLanguageScoreRepository;
    private final ConsentRepository consentRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    /** 없는 이메일로 로그인할 때도 BCrypt 비교를 한 번 하도록 해서 응답 시간 차이로 가입 여부가 드러나지 않게 한다 */
    private final String dummyPasswordHash;

    public AuthService(UserRepository userRepository,
                       UserProfileRepository userProfileRepository,
                       UserLanguageScoreRepository userLanguageScoreRepository,
                       ConsentRepository consentRepository,
                       EmailVerificationRepository emailVerificationRepository,
                       PasswordEncoder passwordEncoder,
                       JwtProvider jwtProvider) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.userLanguageScoreRepository = userLanguageScoreRepository;
        this.consentRepository = consentRepository;
        this.emailVerificationRepository = emailVerificationRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.dummyPasswordHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    // ===== 회원가입 =====

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        try {
            return doSignup(request);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("회원가입 처리 중 예외", e);
            throw new CustomException(ErrorCode.INTERNAL_ERROR, "회원가입 중 오류가 발생했습니다");
        }
    }

    private SignupResponse doSignup(SignupRequest request) {
        String email = request.schoolEmail().trim().toLowerCase();
        LocalDateTime now = LocalDateTime.now(SEOUL);

        // 2. 한국외대 메일인지, 이미 가입된 메일인지
        if (!email.endsWith(SCHOOL_EMAIL_DOMAIN)) {
            throw new CustomException(ErrorCode.NOT_SCHOOL_EMAIL);
        }
        if (userRepository.existsBySchoolEmail(email)) {
            throw new CustomException(ErrorCode.EMAIL_DUPLICATED);
        }

        // 3. 메일 인증 완료 여부 + 인증 후 30분 이내인지
        EmailVerification verification = emailVerificationRepository.findBySchoolEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.EMAIL_NOT_VERIFIED));
        LocalDateTime verifiedAt = verification.getVerifiedAt();
        if (verifiedAt == null || verifiedAt.plusMinutes(VERIFICATION_VALID_MINUTES).isBefore(now)) {
            throw new CustomException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        // 4. 닉네임 중복 (대소문자 무시)
        if (userRepository.existsByNicknameIgnoreCase(request.nickname())) {
            throw new CustomException(ErrorCode.NICKNAME_DUPLICATED);
        }

        // 5. 한 트랜잭션에서 저장
        User user = userRepository.save(User.builder()
                .schoolEmail(email)
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .majorType(request.majorType())
                .build());

        userProfileRepository.save(UserProfile.builder()
                .user(user)
                .gpa(request.gpa())
                .build());

        if (!request.languageScores().isEmpty()) {
            List<UserLanguageScore> scores = request.languageScores().stream()
                    .map(s -> UserLanguageScore.builder()
                            .user(user)
                            .testType(s.testType())
                            .score(s.score())
                            .build())
                    .toList();
            userLanguageScoreRepository.saveAll(scores);
        }

        consentRepository.saveAll(List.of(
                Consent.builder().user(user).consentType(ConsentType.TERMS_OF_SERVICE).consentedAt(now).build(),
                Consent.builder().user(user).consentType(ConsentType.PRIVACY_POLICY).consentedAt(now).build()
        ));

        emailVerificationRepository.delete(verification);

        // 6. 토큰 없이 기본 정보만 반환
        return SignupResponse.from(user);
    }

    // ===== 로그인 =====

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        try {
            return doLogin(request);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("로그인 처리 중 예외", e);
            throw new CustomException(ErrorCode.INTERNAL_ERROR, "로그인 중 오류가 발생했습니다");
        }
    }

    private LoginResponse doLogin(LoginRequest request) {
        String email = request.schoolEmail().trim().toLowerCase();

        User user = userRepository.findBySchoolEmail(email).orElse(null);

        // 이메일이 없든 비밀번호가 틀리든 같은 에러, 비슷한 처리 시간
        String hash = (user != null) ? user.getPassword() : dummyPasswordHash;
        boolean matches = passwordEncoder.matches(request.password(), hash);
        if (user == null || !matches) {
            throw new CustomException(ErrorCode.LOGIN_FAILED);
        }

        String accessToken = jwtProvider.createAccessToken(user.getId());
        return LoginResponse.of(accessToken, jwtProvider.getExpirationSeconds(), user);
    }
}