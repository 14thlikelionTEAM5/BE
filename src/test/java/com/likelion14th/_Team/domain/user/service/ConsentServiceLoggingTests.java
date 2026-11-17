package com.likelion14th._Team.domain.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.likelion14th._Team.domain.user.dto.ConsentRequest;
import com.likelion14th._Team.domain.user.repository.ConsentRepository;
import com.likelion14th._Team.domain.user.repository.UserRepository;
import com.likelion14th._Team.global.enums.ConsentType;
import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class ConsentServiceLoggingTests {
    private final ConsentRepository consentRepository = mock(ConsentRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final ConsentService service = new ConsentService(consentRepository, userRepository);

    @Test
    void recordFailureDoesNotLogExceptionMessage(CapturedOutput output) {
        String marker = UUID.randomUUID().toString();
        when(consentRepository.findByUserIdAndConsentType(1L, ConsentType.EXTERNAL_AI))
                .thenThrow(new IllegalStateException(marker, new IllegalArgumentException(marker)));

        CustomException thrown = assertThrows(CustomException.class,
                () -> service.record(1L, new ConsentRequest("EXTERNAL_AI")));

        assertEquals(ErrorCode.INTERNAL_ERROR, thrown.getErrorCode());
        assertFalse(output.getAll().contains(marker));
        assertTrue(output.getAll().contains("at " + getClass().getName()));
        assertTrue(output.getAll().contains("Caused by:"));
        assertTrue(output.getAll().contains("java.lang.IllegalArgumentException"));
    }
}