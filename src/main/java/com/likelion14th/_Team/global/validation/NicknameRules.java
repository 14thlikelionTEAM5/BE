package com.likelion14th._Team.global.validation;

import java.text.Normalizer;
import java.util.regex.Pattern;

/** 닉네임은 NFC로 정규화한 후 길이·허용 문자를 검사한다. 대소문자와 공백은 보정하지 않는다. */
public final class NicknameRules {
    public static final String REGEXP = "^[가-힣a-zA-Z0-9]{2,12}$";
    public static final String FORMAT_MESSAGE = "2~12자의 한글, 영문, 숫자만 사용할 수 있습니다";
    public static final String CHECK_FORMAT_MESSAGE = "닉네임은 " + FORMAT_MESSAGE;
    private static final Pattern PATTERN = Pattern.compile(REGEXP);

    private NicknameRules() {
    }

    /** 수정 요청의 생략·null은 그대로 두며, 필수 여부는 각 입력 경로에서 검사한다. */
    public static String normalize(String nickname) {
        return nickname == null ? null : Normalizer.normalize(nickname, Normalizer.Form.NFC);
    }

    public static boolean isValidNormalized(String nickname) {
        return nickname != null && PATTERN.matcher(nickname).matches();
    }
}
