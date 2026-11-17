package com.likelion14th._Team.domain.returnreport.service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 외부 AI로 보내기 전 전화번호를 [연락처]로 바꾸는 안전망.
 * 이름·학번은 서버가 구분할 수 없어 사용자의 "직접 지웠음" 확인에 맡긴다.
 * 앞뒤가 숫자인 경우(더 긴 숫자열의 일부)는 전화번호로 보지 않는다.
 */
public final class PhoneNumberMasker {

    static final String REPLACEMENT = "[연락처]";

    private static final List<Pattern> PATTERNS = List.of(
            Pattern.compile("(?<!\\d)01[016789]-?\\d{3,4}-?\\d{4}(?!\\d)"),
            Pattern.compile("(?<!\\d)0\\d{1,2}-?\\d{3,4}-?\\d{4}(?!\\d)"));

    private PhoneNumberMasker() {
    }

    public static String mask(String text) {
        String masked = text;
        for (Pattern pattern : PATTERNS) {
            masked = pattern.matcher(masked).replaceAll(REPLACEMENT);
        }
        return masked;
    }

    public static boolean containsPhoneNumber(String text) {
        return PATTERNS.stream().anyMatch(pattern -> pattern.matcher(text).find());
    }
}
