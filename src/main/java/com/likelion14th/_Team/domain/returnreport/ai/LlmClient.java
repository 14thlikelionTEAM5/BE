package com.likelion14th._Team.domain.returnreport.ai;

import java.util.Map;

/** 외부 AI 호출부. 구현체: GeminiLlmClient(AI_MODE=real), MockLlmClient(AI_MODE=mock, 기본값) */
public interface LlmClient {

    /**
     * 시스템 지시와 사용자 메시지를 보내고, 스키마에 맞춘 JSON 문자열을 받는다.
     * 실패하면 LlmException (메시지에 요청·응답 본문을 넣지 않는다)
     */
    String generateJson(String systemInstruction, String userMessage, Map<String, Object> responseSchema);

    /** 응답 값이 입력 원문에 포함되는지 검증할지 여부. 입력과 무관한 고정 응답(mock)은 false */
    default boolean verifiesSource() {
        return true;
    }

    /** 로그용 설명 (모드·모델) */
    String describe();
}
