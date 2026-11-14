package com.likelion14th._Team.global.exception;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 예상하지 못한 실행 오류의 API별 응답 문구. 업무·검증 예외의 응답은 변경하지 않는다. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiFailure {
    String message();
    String logMessage();
}
