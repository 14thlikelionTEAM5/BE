package com.likelion14th._Team.domain.returnreport.ai;

/**
 * AI 호출 실패. 응답은 항상 E500 하나이고, 실패 종류는 로그 구분용이다.
 * 외부 예외를 cause로 연결하지 않는다 (외부 예외 메시지에 요청·응답 본문이 섞일 수 있음).
 */
public class LlmException extends RuntimeException {

    public enum Kind {
        /** real 모드인데 GEMINI_API_KEY가 비어 있음 */
        MISSING_API_KEY(false),
        /** 401·403 (키 오류·만료) */
        AUTH(false),
        /** 429 */
        RATE_LIMITED(false),
        /** 그 밖의 4xx (요청 형식 오류 등) */
        CLIENT_ERROR(false),
        /** 5xx, 네트워크 오류 */
        UNAVAILABLE(true),
        /** 응답 대기 시간 초과 */
        TIMEOUT(false),
        /** 출력 상한 도달 등으로 결과가 잘림 (status=incomplete). 파싱 실패와 같게 처리 */
        INCOMPLETE(true),
        /** 응답 JSON 파싱 실패, 필수 키 누락 */
        PARSE_FAILED(true),
        /** status=failed (안전 필터 차단 등) */
        FAILED(false),
        /** 모델 출력 텍스트가 없음 */
        EMPTY(false),
        /** 알 수 없는 응답 상태 */
        UNEXPECTED_STATUS(false),
        /** mock 모드의 [MOCK_FAIL] 입력 */
        MOCK_FAILURE(false);

        private final boolean retryable;

        Kind(boolean retryable) {
            this.retryable = retryable;
        }

        public boolean isRetryable() {
            return retryable;
        }
    }

    private final Kind kind;
    private final Integer httpStatus;

    public LlmException(Kind kind) {
        this(kind, null);
    }

    public LlmException(Kind kind, Integer httpStatus) {
        super("AI 호출 실패: " + kind + (httpStatus == null ? "" : " (HTTP " + httpStatus + ")"), null, false, false);
        this.kind = kind;
        this.httpStatus = httpStatus;
    }

    public Kind getKind() {
        return kind;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }
}
