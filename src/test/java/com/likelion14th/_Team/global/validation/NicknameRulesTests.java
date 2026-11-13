package com.likelion14th._Team.global.validation;

import com.likelion14th._Team.domain.auth.dto.SignupRequest;
import com.likelion14th._Team.domain.user.dto.MyInfoUpdateRequest;
import jakarta.validation.Validation;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NicknameRulesTests {
    private SignupRequest signup(String nickname) {
        return new SignupRequest("test@hufs.ac.kr", "test-password", nickname, null, null,
                new BigDecimal("4.0"), null);
    }

    private MyInfoUpdateRequest update(String nickname) {
        return new MyInfoUpdateRequest(nickname, null, null, null);
    }

    private boolean validNickname(Object request) {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            return factory.getValidator().validate(request).stream()
                    .noneMatch(error -> error.getPropertyPath().toString().equals("nickname"));
        }
    }

    @Test
    void normalizedLengthIsUsedEvenWhenNfdExceedsTwelveCharacters() {
        for (String nfc : List.of("채원", "가".repeat(12), "Chaewon01")) {
            String nfd = Normalizer.normalize(nfc, Normalizer.Form.NFD);
            assertEquals(nfc, NicknameRules.normalize(nfd));
            assertEquals(nfc, signup(nfd).nickname());
            assertEquals(nfc, update(nfd).nickname());
            assertTrue(NicknameRules.isValidNormalized(NicknameRules.normalize(nfd)));
            assertTrue(validNickname(signup(nfd)));
            assertTrue(validNickname(update(nfd)));
        }
    }

    @Test
    void allInputPathsRejectTheSameInvalidFormats() {
        for (String input : List.of("", "가", "가".repeat(13), "ㅋㅋ", "ㅏㅏ", "채 원", " 채원", "채원 ", "name!", "😀😀")) {
            String nfd = Normalizer.normalize(input, Normalizer.Form.NFD);
            assertFalse(NicknameRules.isValidNormalized(NicknameRules.normalize(nfd)));
            assertFalse(validNickname(signup(nfd)));
            assertFalse(validNickname(update(nfd)));
        }
    }

    @Test
    void optionalNullAndOtherPatchFieldsAreNotChanged() {
        assertNull(NicknameRules.normalize(null));
        assertFalse(NicknameRules.isValidNormalized(null));
        assertFalse(validNickname(signup(null)));
        assertNull(update(null).nickname());
        assertNull(update(null).languageScores());
        assertTrue(validNickname(update(null)));
        assertTrue(new MyInfoUpdateRequest(null, null, null, List.of()).languageScores().isEmpty());
        assertEquals("Chaewon", NicknameRules.normalize("Chaewon"));
    }
}
