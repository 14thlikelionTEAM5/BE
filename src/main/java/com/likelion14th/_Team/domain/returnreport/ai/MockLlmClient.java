package com.likelion14th._Team.domain.returnreport.ai;

import java.time.Duration;
import java.util.Map;

/**
 * AI_MODE=mock(기본값). 입력과 무관하게 고정 응답을 반환한다.
 * 텍스트가 [MOCK_FAIL]로 시작하면 실패시켜 FE가 실패 화면을 확인할 수 있게 한다.
 */
public class MockLlmClient implements LlmClient {

    static final String FAIL_TRIGGER = "[MOCK_FAIL]";

    static final String FIXED_RESPONSE = """
            {
              "cost": {"airfare": "왕복 약 90만원", "insurance": "약 30만원", "monthlyLiving": "월 150만원", "total": "약 1,200만원"},
              "housing": "DORM",
              "visa": {"duration": "약 2주", "type": "Student Pass"},
              "insuranceProvider": "삼성화재",
              "courses": [
                {"code": "MKT2101", "nameEn": "Principles of Marketing", "foreignCredit": 4.0, "satisfaction": 4},
                {"code": "FIN2201", "nameEn": "Corporate Finance", "foreignCredit": 4.0, "satisfaction": 3},
                {"code": null, "nameEn": "Asian Business Environment", "foreignCredit": 2.0, "satisfaction": null}
              ],
              "summary": "학교 기숙사에서 지내서 통학이 편했어요. 마케팅 수업은 팀 프로젝트가 많았고 난이도는 보통이었어요. 생활비는 한국보다 조금 더 들었어요."
            }
            """;

    private final Duration delay;

    public MockLlmClient(Duration delay) {
        this.delay = delay;
    }

    @Override
    public String generateJson(String systemInstruction, String userMessage, Map<String, Object> responseSchema) {
        sleep();
        if (ExtractReturnReportPrompt.reportText(userMessage).stripLeading().startsWith(FAIL_TRIGGER)) {
            throw new LlmException(LlmException.Kind.MOCK_FAILURE);
        }
        return FIXED_RESPONSE;
    }

    @Override
    public boolean verifiesSource() {
        return false;
    }

    @Override
    public String describe() {
        return "mock";
    }

    private void sleep() {
        if (delay.isZero()) {
            return;
        }
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
