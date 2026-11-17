package com.likelion14th._Team.domain.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.likelion14th._Team.domain.user.dto.PasswordChangeRequest;
import com.likelion14th._Team.domain.user.repository.UserRepository;
import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(OutputCaptureExtension.class)
class PasswordServiceLoggingTests {
    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordService service = new PasswordService(userRepository, mock(PasswordEncoder.class));

    @Test
    void changePasswordFailureDoesNotLogExceptionMessage(CapturedOutput output) {
        String marker = UUID.randomUUID().toString();
        when(userRepository.findById(1L))
                .thenThrow(new IllegalStateException(marker, new IllegalArgumentException(marker)));

        CustomException thrown = assertThrows(CustomException.class,
                () -> service.changePassword(1L, new PasswordChangeRequest("Passhufs1234!", "Newpass5678!")));

        assertEquals(ErrorCode.INTERNAL_ERROR, thrown.getErrorCode());
        assertFalse(output.getAll().contains(marker));
        assertTrue(output.getAll().contains("at " + getClass().getName()));
        assertTrue(output.getAll().contains("Caused by:"));
        assertTrue(output.getAll().contains("java.lang.IllegalArgumentException"));
    }
}