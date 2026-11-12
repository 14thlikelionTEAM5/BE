package com.likelion14th._Team.global.exception;

import org.springframework.http.HttpStatus;

/**
 * API 명세서의 Error Cases 표와 1:1로 맞춘 에러 목록.
 * code는 명세의 E400 / E401 ... 이고, 같은 code라도 message가 다를 수 있어 항목을 나눴다.
 * 새 API의 에러가 필요하면 여기에 추가한다.
 */
public enum ErrorCode {

    // 400
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "E400", "입력값이 올바르지 않습니다"),
    NOT_SCHOOL_EMAIL(HttpStatus.BAD_REQUEST, "E400", "학교 메일(@hufs.ac.kr)만 사용할 수 있습니다"),
    INVALID_CODE_FORMAT(HttpStatus.BAD_REQUEST, "E400", "인증 코드는 6자리 숫자여야 합니다"),
    CODE_MISMATCH(HttpStatus.BAD_REQUEST, "E400", "인증 코드가 일치하지 않습니다"),
    CODE_EXPIRED_OR_MISSING(HttpStatus.BAD_REQUEST, "E400", "인증 코드가 만료되었거나 발송 이력이 없습니다"),

    // 401
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "E401", "학교 메일 또는 비밀번호가 올바르지 않습니다"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "E401", "로그인이 필요합니다."),
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "E401", "로그인이 필요합니다."),
    STUDY_PLAN_LOGIN_REQUIRED(HttpStatus.UNAUTHORIZED, "E401", "로그인이 필요합니다."),
    UNIVERSITY_LOGIN_REQUIRED(HttpStatus.UNAUTHORIZED, "E401", "로그인이 필요합니다."),
    UNIVERSITY_COUNTRIES_LOGIN_REQUIRED(HttpStatus.UNAUTHORIZED, "E401", "로그인이 필요합니다."),

    // 403
    EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN, "E403", "학교 메일 인증이 완료되지 않았거나 만료되었습니다"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "E403", "접근 권한이 없습니다"),
    STUDY_PLAN_FORBIDDEN(HttpStatus.FORBIDDEN, "E403", "해당 수강계획에 대한 권한이 없습니다."),
    RETURN_REPORT_FORBIDDEN(HttpStatus.FORBIDDEN, "E403", "본인의 귀국보고서만 수정할 수 있습니다"),
    EXTERNAL_AI_CONSENT_REQUIRED(HttpStatus.FORBIDDEN, "E403", "외부 AI 처리 동의가 필요합니다"),
    RETURN_REPORT_COURSE_FORBIDDEN(HttpStatus.FORBIDDEN, "E403", "본인의 귀국보고서 과목만 등록할 수 있습니다"),
    COURSE_CASE_FORBIDDEN(HttpStatus.FORBIDDEN, "E403", "본인의 인정 사례만 수정할 수 있습니다."),
    ADMISSION_REPORT_FORBIDDEN(HttpStatus.FORBIDDEN, "E403", "본인의 스펙만 수정할 수 있습니다"),

    // 404
    BOOKMARK_NOT_FOUND(HttpStatus.NOT_FOUND, "E404", "찜하지 않은 학교입니다"),
    UNIVERSITY_NOT_FOUND(HttpStatus.NOT_FOUND, "E404", "대학을 찾을 수 없습니다."),
    STUDY_PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "E404", "수강계획을 찾을 수 없습니다."),
    SOURCE_COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "E404", "원본 과목을 찾을 수 없습니다."),
    HOME_COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "E404", "본교 과목을 찾을 수 없습니다."),
    STUDY_PLAN_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "E404", "수강계획 과목을 찾을 수 없습니다."),
    RETURN_REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "E404", "존재하지 않는 귀국보고서입니다"),
    RETURN_REPORT_COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "E404", "존재하지 않는 귀국보고서 과목입니다"),
    COURSE_CASE_NOT_FOUND(HttpStatus.NOT_FOUND, "E404", "존재하지 않는 인정 사례입니다"),
    ADMISSION_REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "E404", "존재하지 않는 스펙입니다"),

    // 409
    BOOKMARK_DUPLICATED(HttpStatus.CONFLICT, "E409", "이미 찜한 학교입니다"),
    EMAIL_DUPLICATED(HttpStatus.CONFLICT, "E409", "이미 가입된 학교 메일입니다"),
    NICKNAME_DUPLICATED(HttpStatus.CONFLICT, "E409", "이미 사용 중인 닉네임입니다"),
    STUDY_PLAN_UNIVERSITY_DUPLICATED(HttpStatus.CONFLICT, "E409", "이미 해당 학교의 수강계획이 있어요"),
    STUDY_PLAN_LIMIT_REACHED(HttpStatus.CONFLICT, "E409", "수강계획은 최대 5개까지 생성할 수 있습니다."),
    STUDY_PLAN_ITEM_DUPLICATED(HttpStatus.CONFLICT, "E409", "이미 수강계획에 추가된 과목입니다."),
    RETURN_REPORT_DUPLICATED(HttpStatus.CONFLICT, "E409", "이미 해당 학기의 귀국보고서를 등록했습니다"),
    COURSE_CASE_DUPLICATED(HttpStatus.CONFLICT, "E409", "이미 등록된 인정 사례입니다."),
    ADMISSION_REPORT_DUPLICATED(HttpStatus.CONFLICT, "E409", "이미 같은 학교·학기의 같은 유형을 등록했습니다"),

    // 429
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "E429", "잠시 후 다시 요청해 주세요"),

    // 500 (API별 문구가 다르므로 필요한 곳에서 CustomException(errorCode, message)로 문구를 덮어쓴다)
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E500", "서버 오류가 발생했습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
