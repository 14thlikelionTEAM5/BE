package com.likelion14th._Team.domain.university.dto;

import com.likelion14th._Team.global.enums.LanguageTestType;
import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;

public record UniversitySearch(String keyword, String country, LanguageTestType languageTest,
                               boolean eligibleOnly, String sort) {
    public static UniversitySearch from(String keyword, String country, String languageTest,
                                        String eligibleOnly, String sort) {
        try {
            if (eligibleOnly != null && !eligibleOnly.equals("true") && !eligibleOnly.equals("false")) {
                throw new IllegalArgumentException();
            }
            String order = sort == null ? "NAME" : sort;
            if (!order.equals("NAME") && !order.equals("COUNTRY")) throw new IllegalArgumentException();
            String normalizedKeyword = keyword == null ? null : keyword.strip();
            if ((normalizedKeyword != null
                    && normalizedKeyword.codePointCount(0, normalizedKeyword.length()) > 200)
                    || (country != null && country.length() > 100)) {
                throw new IllegalArgumentException();
            }
            return new UniversitySearch(normalizedKeyword,
                    country == null ? null : country.strip(),
                    languageTest == null ? null : LanguageTestType.valueOf(languageTest),
                    "true".equals(eligibleOnly), order);
        } catch (IllegalArgumentException exception) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "검색 조건이 올바르지 않습니다.");
        }
    }
}
