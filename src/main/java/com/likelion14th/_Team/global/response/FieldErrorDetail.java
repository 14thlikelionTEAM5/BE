package com.likelion14th._Team.global.response;

/**
 * 입력값 검증 실패(E400) 시 응답의 errors 배열에 담기는 항목.
 * 예: { "field": "nickname", "reason": "2~12자의 한글, 영문, 숫자만 사용할 수 있습니다" }
 */
public record FieldErrorDetail(String field, String reason) {
}