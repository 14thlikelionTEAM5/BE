package com.likelion14th._Team.global.util;

import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import com.likelion14th._Team.global.response.FieldErrorDetail;
import java.util.List;

/**
 * 자동완성 검색어(keyword) 공통 검증.
 * 앞뒤 공백을 제거한 뒤 1~50자여야 하며, 검색에는 공백을 제거한 값을 사용한다.
 */
public final class SearchKeyword {

    public static final String FIELD = "keyword";
    public static final int MAX_LENGTH = 50;

    private SearchKeyword() {
    }

    /** 검증 실패 사유를 반환한다. 통과하면 null */
    public static FieldErrorDetail check(String raw) {
        if (raw == null || raw.isBlank()) {
            return new FieldErrorDetail(FIELD, "검색어를 입력해 주세요");
        }
        String trimmed = raw.trim();
        if (trimmed.codePointCount(0, trimmed.length()) > MAX_LENGTH) {
            return new FieldErrorDetail(FIELD, "검색어는 " + MAX_LENGTH + "자 이하로 입력해 주세요");
        }
        return null;
    }

    /** 검증 후 앞뒤 공백을 제거한 검색어를 반환한다. 실패하면 E400 */
    public static String require(String raw) {
        FieldErrorDetail error = check(raw);
        if (error != null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, ErrorCode.INVALID_INPUT.getMessage(), List.of(error));
        }
        return raw.trim();
    }
}
