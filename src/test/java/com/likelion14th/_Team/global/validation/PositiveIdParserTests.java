package com.likelion14th._Team.global.validation;

import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import com.likelion14th._Team.global.response.FieldErrorDetail;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PositiveIdParserTests {
    @Test
    void preservesPositiveLongRangeAndLeadingZeroBehavior() {
        assertEquals(1L, PositiveIdParser.parse("1", "universityId"));
        assertEquals(1L, PositiveIdParser.parse("01", "universityId"));
        assertEquals(Long.MAX_VALUE, PositiveIdParser.parse(Long.toString(Long.MAX_VALUE), "universityId"));
    }

    @Test
    void invalidInputRetainsFieldErrors() {
        String[] invalid = {null, "", "0", "00", "-1", "+1", "1.0", "abc", " 1", "1 ", "１", "9223372036854775808"};
        for (String value : invalid) {
            CustomException error = assertThrows(CustomException.class,
                    () -> PositiveIdParser.parse(value, "universityId"));
            assertEquals(ErrorCode.INVALID_INPUT, error.getErrorCode());
            assertEquals("입력값이 올바르지 않습니다", error.getMessage());
            assertEquals(List.of(new FieldErrorDetail("universityId", "양의 정수를 입력해 주세요.")), error.getErrors());
        }
    }
}
