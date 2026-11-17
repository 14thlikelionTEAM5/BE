package com.likelion14th._Team.domain.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.likelion14th._Team.domain.auth.dto.EmailSendRequest;
import com.likelion14th._Team.domain.auth.dto.EmailVerifyRequest;
import com.likelion14th._Team.domain.user.repository.EmailVerificationRepository;
import com.likelion14th._Team.domain.user.repository.UserRepository;
import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(OutputCaptureExtension.class)
class EmailAuthServiceLoggingTests {
    private UserRepository userRepository;
    private EmailVerificationRepository emailVerificationRepository;
    private EmailAuthService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        emailVerificationRepository = mock(EmailVerificationRepository.class);
        service = new EmailAuthService(
                userRepository,
                emailVerificationRepository,
                mock(PasswordEncoder.class),
                mock(VerificationMailSender.class));
    }

    @Test
    void sendCodeFailureDoesNotLogExceptionMessage(CapturedOutput output) {
        String marker = UUID.randomUUID().toString();
        when(userRepository.existsBySchoolEmail("user@hufs.ac.kr"))
                .thenThrow(new IllegalStateException(marker, new IllegalArgumentException(marker)));

        CustomException thrown = assertThrows(CustomException.class,
                () -> service.sendCode(new EmailSendRequest("user@hufs.ac.kr")));

        assertEquals(ErrorCode.INTERNAL_ERROR, thrown.getErrorCode());
        assertLogHasStackButNoMarker(output, marker);
    }

    @Test
    void verifyCodeFailureDoesNotLogExceptionMessage(CapturedOutput output) {
        String marker = UUID.randomUUID().toString();
        when(emailVerificationRepository.findBySchoolEmail("user@hufs.ac.kr"))
                .thenThrow(new IllegalStateException(marker, new IllegalArgumentException(marker)));

        CustomException thrown = assertThrows(CustomException.class,
                () -> service.verifyCode(new EmailVerifyRequest("user@hufs.ac.kr", "123456")));

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