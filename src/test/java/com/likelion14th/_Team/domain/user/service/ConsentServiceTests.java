package com.likelion14th._Team.domain.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.likelion14th._Team.domain.user.dto.ConsentRequest;
import com.likelion14th._Team.domain.user.dto.ConsentResult;
import com.likelion14th._Team.domain.user.entity.Consent;
import com.likelion14th._Team.domain.user.entity.User;
import com.likelion14th._Team.domain.user.repository.ConsentRepository;
import com.likelion14th._Team.domain.user.repository.UserRepository;
import com.likelion14th._Team.global.enums.ConsentType;
import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

class ConsentServiceTests {

    private static final Long USER_ID = 1L;
    private static final ConsentRequest REQUEST = new ConsentRequest("EXTERNAL_AI");
    private static final LocalDateTime CONSENTED_AT = LocalDateTime.of(2026, 10, 30, 19, 2, 0);

    private final ConsentRepository consentRepository = mock(ConsentRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final ConsentService service = new ConsentService(consentRepository, userRepository);
    private final User user = mock(User.class);

    @Test
    void savesNewConsentAndReturnsCreated() {
        when(consentRepository.findByUserIdAndConsentType(USER_ID, ConsentType.EXTERNAL_AI))
                .thenReturn(Optional.empty());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(consentRepository.saveAndFlush(any(Consent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConsentResult result = service.record(USER_ID, REQUEST);

        assertTrue(result.created());
        assertEquals(ConsentType.EXTERNAL_AI, result.consent().consentType());
        assertNotNull(result.consent().consentedAt());
        assertEquals(0, result.consent().consentedAt().getNano());
        ArgumentCaptor<Consent> saved = ArgumentCaptor.forClass(Consent.class);
        verify(consentRepository).saveAndFlush(saved.capture());
        assertSame(user, saved.getValue().getUser());
    }

    @Test
    void returnsExistingConsentWithoutSaving() {
        when(consentRepository.findByUserIdAndConsentType(USER_ID, ConsentType.EXTERNAL_AI))
                .thenReturn(Optional.of(consent()));

        ConsentResult result = service.record(USER_ID, REQUEST);

        assertFalse(result.created());
        assertEquals(CONSENTED_AT, result.consent().consentedAt());
        verify(consentRepository, never()).saveAndFlush(any());
        verifyNoInteractions(userRepository);
    }

    @Test
    void returnsExistingConsentWhenConcurrentRequestSavedFirst() {
        when(consentRepository.findByUserIdAndConsentType(USER_ID, ConsentType.EXTERNAL_AI))
                .thenReturn(Optional.empty(), Optional.of(consent()));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(consentRepository.saveAndFlush(any(Consent.class)))
                .thenThrow(new DataIntegrityViolationException("uk_consent_user_type"));

        ConsentResult result = service.record(USER_ID, REQUEST);

        assertFalse(result.created());
        assertEquals(CONSENTED_AT, result.consent().consentedAt());
    }

    @Test
    void rejectsMissingOrUnsupportedConsentType() {
        ConsentRequest[] requests = {null, new ConsentRequest(null), new ConsentRequest(" "),
                new ConsentRequest("TERMS_OF_SERVICE"), new ConsentRequest("PRIVACY_POLICY"),
                new ConsentRequest("external_ai")};
        for (ConsentRequest request : requests) {
            CustomException e = assertThrows(CustomException.class, () -> service.record(USER_ID, request));
            assertEquals(ErrorCode.INVALID_INPUT, e.getErrorCode());
            assertEquals("consentType", e.getErrors().getFirst().field());
        }
        verifyNoInteractions(consentRepository, userRepository);
    }

    @Test
    void missingUserReturns401() {
        when(consentRepository.findByUserIdAndConsentType(USER_ID, ConsentType.EXTERNAL_AI))
                .thenReturn(Optional.empty());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        CustomException e = assertThrows(CustomException.class, () -> service.record(USER_ID, REQUEST));

        assertEquals(ErrorCode.UNAUTHORIZED, e.getErrorCode());
        verify(consentRepository, never()).saveAndFlush(any());
    }

    @Test
    void wrapsUnexpectedFailureAs500() {
        when(consentRepository.findByUserIdAndConsentType(USER_ID, ConsentType.EXTERNAL_AI))
                .thenThrow(new IllegalStateException("db down"));

        CustomException e = assertThrows(CustomException.class, () -> service.record(USER_ID, REQUEST));

        assertEquals(ErrorCode.INTERNAL_ERROR, e.getErrorCode());
        assertEquals("동의 기록 중 오류가 발생했습니다.", e.getMessage());
    }

    @Test
    void duplicateWithoutExistingRecordIsWrappedAs500() {
        when(consentRepository.findByUserIdAndConsentType(USER_ID, ConsentType.EXTERNAL_AI))
                .thenReturn(Optional.empty());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(consentRepository.saveAndFlush(any(Consent.class)))
                .thenThrow(new DataIntegrityViolationException("fk"));

        CustomException e = assertThrows(CustomException.class, () -> service.record(USER_ID, REQUEST));

        assertEquals(ErrorCode.INTERNAL_ERROR, e.getErrorCode());
    }

    private Consent consent() {
        return Consent.builder().user(user).consentType(ConsentType.EXTERNAL_AI).consentedAt(CONSENTED_AT).build();
    }
    @Test
    void requireExternalAiConsentPassesWhenConsentExists() {
        when(consentRepository.existsByUserIdAndConsentType(USER_ID, ConsentType.EXTERNAL_AI)).thenReturn(true);

        service.requireExternalAiConsent(USER_ID);
    }

    @Test
    void requireExternalAiConsentThrows403WhenConsentMissing() {
        when(consentRepository.existsByUserIdAndConsentType(USER_ID, ConsentType.EXTERNAL_AI)).thenReturn(false);

        CustomException e = assertThrows(CustomException.class, () -> service.requireExternalAiConsent(USER_ID));

        assertEquals(ErrorCode.EXTERNAL_AI_CONSENT_REQUIRED, e.getErrorCode());
    }
}
