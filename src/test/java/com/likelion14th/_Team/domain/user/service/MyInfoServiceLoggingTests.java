package com.likelion14th._Team.domain.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.likelion14th._Team.domain.user.dto.MyInfoUpdateRequest;
import com.likelion14th._Team.domain.user.repository.ConsentRepository;
import com.likelion14th._Team.domain.user.repository.UserLanguageScoreRepository;
import com.likelion14th._Team.domain.user.repository.UserProfileRepository;
import com.likelion14th._Team.domain.user.repository.UserRepository;
import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class MyInfoServiceLoggingTests {
    private final UserRepository userRepository = mock(UserRepository.class);
    private final MyInfoService service = new MyInfoService(
            userRepository,
            mock(UserProfileRepository.class),
            mock(UserLanguageScoreRepository.class),
            mock(ConsentRepository.class));

    @Test
    void getMyInfoFailureDoesNotLogExceptionMessage(CapturedOutput output) {
        String marker = UUID.randomUUID().toString();
        when(userRepository.findById(1L))
                .thenThrow(new IllegalStateException(marker, new IllegalArgumentException(marker)));

        CustomException thrown = assertThrows(CustomException.class, () -> service.getMyInfo(1L));

        assertEquals(ErrorCode.INTERNAL_ERROR, thrown.getErrorCode());
        assertLogHasStackButNoMarker(output, marker);
    }

    @Test
    void updateFailureDoesNotLogExceptionMessage(CapturedOutput output) {
        String marker = UUID.randomUUID().toString();
        when(userRepository.findByIdForUpdate(1L))
                .thenThrow(new IllegalStateException(marker, new IllegalArgumentException(marker)));

        CustomException thrown = assertThrows(CustomException.class,
                () -> service.update(1L, mock(MyInfoUpdateRequest.class)));

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