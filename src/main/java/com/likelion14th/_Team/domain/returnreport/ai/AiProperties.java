package com.likelion14th._Team.domain.returnreport.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** application.yaml의 ai.* 설정. API 키는 환경변수(GEMINI_API_KEY)로만 받는다 */
@ConfigurationProperties(prefix = "ai")
public record AiProperties(
        /** real / mock */
        String mode,
        Prompt prompt,
        Gemini gemini
) {

    public record Prompt(
            /** 귀국보고서 추출 프롬프트 버전. prompts/extract_return_report_{버전}.txt */
            String extractReturnReport
    ) {
    }

    public record Gemini(
            String apiKey,
            String model,
            String baseUrl,
            /** Interactions API의 Api-Revision 헤더 값 */
            String apiRevision,
            int connectTimeoutSeconds,
            int timeoutSeconds,
            int totalTimeoutSeconds,
            String thinkingLevel,
            int maxOutputTokens
    ) {

        /** 키 값이 로그에 찍히지 않도록 toString에서 숨긴다 */
        @Override
        public String toString() {
            return "Gemini[model=" + model + ", apiKey=" + (apiKey == null || apiKey.isBlank() ? "(empty)" : "(set)")
                    + ", apiRevision=" + apiRevision + "]";
        }
    }

    public boolean isRealMode() {
        return mode != null && "real".equalsIgnoreCase(mode.trim());
    }
}
