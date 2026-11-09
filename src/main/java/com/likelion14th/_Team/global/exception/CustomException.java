package com.likelion14th._Team.global.exception;

/**
 * 서비스 로직에서 에러 응답을 내려야 할 때 던지는 예외.
 * 예) throw new CustomException(ErrorCode.EMAIL_DUPLICATED);
 * 예) throw new CustomException(ErrorCode.INTERNAL_ERROR, "회원가입 중 오류가 발생했습니다");
 */
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /** ErrorCode의 기본 문구 대신 API별 문구를 쓰고 싶을 때 */
    public CustomException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}