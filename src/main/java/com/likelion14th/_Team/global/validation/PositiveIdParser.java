package com.likelion14th._Team.global.validation;

import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import com.likelion14th._Team.global.response.FieldErrorDetail;
import java.util.List;

/** 숫자로만 구성된 양의 Long ID를 검증한다. 공백 제거 등 입력 보정은 하지 않는다. */
public final class PositiveIdParser {
    private PositiveIdParser() {
    }

    public static long parse(String value, String field) {
        try {
            if (value == null || !value.matches("[0-9]+")) {
                throw new NumberFormatException();
            }
            long id = Long.parseLong(value);
            if (id <= 0) {
                throw new NumberFormatException();
            }
            return id;
        } catch (NumberFormatException exception) {
            throw new CustomException(ErrorCode.INVALID_INPUT, ErrorCode.INVALID_INPUT.getMessage(),
                    List.of(new FieldErrorDetail(field, "양의 정수를 입력해 주세요.")));
        }
    }
}
