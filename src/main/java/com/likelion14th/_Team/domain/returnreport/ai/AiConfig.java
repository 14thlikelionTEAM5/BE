package com.likelion14th._Team.domain.returnreport.ai;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI_MODE에 따라 LlmClient 구현체를 고른다. real이 아니면 mock.
 * 키가 비어 있어도 앱은 정상 시작한다 (추출 요청 시 ERROR 로그 + E500).
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfig {

    private static final Duration MOCK_DELAY = Duration.ofSeconds(1);

    @Bean
    public LlmClient llmClient(AiProperties properties) {
        if (properties.isRealMode()) {
            String apiKey = properties.gemini().apiKey();
            if (apiKey == null || apiKey.isBlank()) {
                log.error("AI_MODE=real이지만 GEMINI_API_KEY가 비어 있습니다. 귀국보고서 항목 추출 요청은 실패합니다.");
            }
            log.info("귀국보고서 항목 추출: Gemini 사용 (model={})", properties.gemini().model());
            return new GeminiLlmClient(properties.gemini());
        }
        if (properties.mode() != null && !"mock".equalsIgnoreCase(properties.mode().trim())) {
            log.warn("알 수 없는 AI_MODE 값입니다. mock으로 동작합니다.");
        }
        log.warn("AI_MODE=mock: 귀국보고서 항목 추출은 고정 응답을 반환합니다. 운영 환경이면 AI_MODE=real을 확인하세요.");
        return new MockLlmClient(MOCK_DELAY);
    }

    @Bean
    public ExtractReturnReportPrompt extractReturnReportPrompt(AiProperties properties) {
        return new ExtractReturnReportPrompt(properties.prompt().extractReturnReport());
    }
}
