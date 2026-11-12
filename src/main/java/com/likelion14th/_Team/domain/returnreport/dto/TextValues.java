package com.likelion14th._Team.domain.returnreport.dto;

/** 귀국보고서 문자열 공통 처리: 앞뒤 공백을 제거하고, 비어 있으면 null */
public final class TextValues {

    private TextValues() {
    }

    public static String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
