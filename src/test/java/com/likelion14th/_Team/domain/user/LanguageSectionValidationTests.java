package com.likelion14th._Team.domain.user;

import static org.junit.jupiter.api.Assertions.*;

import com.likelion14th._Team.domain.auth.dto.SignupRequest.LanguageScoreRequest;
import com.likelion14th._Team.domain.user.entity.LanguageSectionScores;
import com.likelion14th._Team.global.enums.LanguageTestType;
import jakarta.validation.Validation;
import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

class LanguageSectionValidationTests {
    @ParameterizedTest
    @CsvSource({"IELTS,6.5,0,true", "IELTS,6.5,9,true", "IELTS,6.5,6.25,false",
            "IELTS,6.5,-0.5,false", "IELTS,6.5,9.5,false",
            "TOEFL_IBT,90,0,true", "TOEFL_IBT,90,30,true", "TOEFL_IBT,90,30.5,false",
            "TOEFL_IBT,90,20.5,false", "TOEFL_NEW,4.5,1,true", "TOEFL_NEW,4.5,6,true",
            "TOEFL_NEW,4.5,0.5,false", "TOEFL_NEW,4.5,4.25,false", "TOEFL_ITP,550,20,false"})
    void validatesSectionsUsingTheSelectedScale(LanguageTestType type, String total, String section, boolean valid) {
        var value = new BigDecimal(section);
        var request = new LanguageScoreRequest(type, new BigDecimal(total),
                new LanguageSectionScores(value, value, value, value));
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertEquals(valid, factory.getValidator().validate(request).isEmpty());
        }
    }

    @Test
    void legacyTotalOnlyIsAllowedButPartialObjectHasFieldErrors() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertTrue(validator.validate(new LanguageScoreRequest(LanguageTestType.IELTS, new BigDecimal("6.5"))).isEmpty());
            var errors = validator.validate(new LanguageScoreRequest(LanguageTestType.IELTS, new BigDecimal("6.5"),
                    new LanguageSectionScores(new BigDecimal("6"), null, null, null)));
            assertTrue(errors.stream().anyMatch(e -> e.getPropertyPath().toString().equals("sectionScores.listening")));
            assertEquals(3, errors.size());
        }
    }
}
