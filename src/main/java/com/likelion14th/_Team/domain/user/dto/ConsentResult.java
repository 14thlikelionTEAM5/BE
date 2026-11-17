package com.likelion14th._Team.domain.user.dto;

/** 동의 기록 결과. created가 false면 이미 있던 기록을 그대로 반환한 것 */
public record ConsentResult(
        ConsentResponse consent,
        boolean created
) {
}
