package com.likelion14th._Team.global.response;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 모든 API가 공통으로 사용하는 응답 구조.
 * { code, message, data, errors, timestamp }
 * 성공 시 errors는 null, 실패 시 data는 null.
 */
public record ApiResponse<T>(
        String code,
        String message,
        T data,
        List<FieldErrorDetail> errors,
        String timestamp
) {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    /** 200 OK 성공 응답 */
    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>("S200", message, data, null, now());
    }

    /** 201 Created 성공 응답 */
    public static <T> ApiResponse<T> created(String message, T data) {
        return new ApiResponse<>("S201", message, data, null, now());
    }

    /** 에러 응답. errors는 입력값 검증 실패일 때만 채우고, 그 외에는 null */
    public static ApiResponse<Void> error(String code, String message, List<FieldErrorDetail> errors) {
        return new ApiResponse<>(code, message, null, errors, now());
    }

    /** 명세 예시와 같은 형식 (예: 2026-10-29T16:58:00) */
    public static String now() {
        return LocalDateTime.now(SEOUL)
                .withNano(0)
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}