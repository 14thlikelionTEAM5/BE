package com.likelion14th._Team.global.security;

import com.likelion14th._Team.global.exception.ErrorCode;
import com.likelion14th._Team.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 시큐리티 필터 단계에서 발생한 401/403을 공통 응답 형식의 JSON으로 내려준다.
 * (컨트롤러 이전 단계라 GlobalExceptionHandler가 처리하지 못한다)
 */
final class SecurityErrorWriter {

    private SecurityErrorWriter() {
    }

    static void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType("application/json;charset=UTF-8");

        String body = "{\"code\":\"" + escape(errorCode.getCode()) + "\","
                + "\"message\":\"" + escape(errorCode.getMessage()) + "\","
                + "\"data\":null,"
                + "\"errors\":null,"
                + "\"timestamp\":\"" + ApiResponse.now() + "\"}";

        response.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}