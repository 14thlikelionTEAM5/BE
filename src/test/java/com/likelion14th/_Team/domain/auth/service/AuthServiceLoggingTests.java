package com.likelion14th._Team.domain.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.likelion14th._Team.domain.auth.dto.LoginRequest;
import com.likelion14th._Team.domain.auth.dto.SignupRequest;
import com.likelion14th._Team.domain.user.repository.ConsentRepository;
import com.likelion14th._Team.domain.user.repository.EmailVerificationRepository;
import com.likelion14th._Team.domain.user.repository.UserLanguageScoreRepository;
import com.likelion14th._Team.domain.user.repository.UserProfileRepository;
import com.likelion14th._Team.domain.user.repository.UserRepository;
import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import com.likelion14th._Team.global.security.JwtProvider;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(OutputCaptureExtension.class)
class AuthServiceLoggingTests {
    private UserRepository userRepository;
    private AuthService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        service = new AuthService(
                userRepository,
                mock(UserProfileRepository.class),
                mock(UserLanguageScoreRepository.class),
                mock(ConsentRepository.class),
                mock(EmailVerificationRepository.class),
                mock(PasswordEncoder.class),
                mock(JwtProvider.class));
    }

    @Test
    void loginFailureDoesNotLogExceptionMessage(CapturedOutput output) {
        String marker = UUID.randomUUID().toString();
        when(userRepository.findBySchoolEmail("user@hufs.ac.kr"))
                .thenThrow(new IllegalStateException(marker, new IllegalArgumentException(marker)));

        CustomException thrown = assertThrows(CustomException.class,
                () -> service.login(new LoginRequest("user@hufs.ac.kr", marker)));

        assertEquals(ErrorCode.INTERNAL_ERROR, thrown.getErrorCode());
        assertLogHasStackButNoMarker(output, marker);
    }

    @Test
    void signupFailureDoesNotLogExceptionMessage(CapturedOutput output) {
        String marker = UUID.randomUUID().toString();
        SignupRequest request = mock(SignupRequest.class);
        when(request.schoolEmail())
                .thenThrow(new IllegalStateException(marker, new IllegalArgumentException(marker)));

        CustomException thrown = assertThrows(CustomException.class, () -> service.signup(request));

        assertEquals(ErrorCode.INTERNAL_ERROR, thrown.getErrorCode());
        assertLogHasStackButNoMarker(output, marker);
    }

    private void assertLogHasStackButNoMarker(CapturedOutput output, String marker) {
        assertFalse(output.getAll().contains(marker));
        assertTrue(output.getAll().contains("at " + getClass().getName()));
        assertTrue(output.getAll().contains("Caused by:"));
        assertTrue(output.getAll().contains("java.lang.IllegalArgumentException"));
    }
}