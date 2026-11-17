package com.likelion14th._Team.domain.returnreport.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class MockLlmClientTests {

    private final MockLlmClient client = new MockLlmClient(Duration.ZERO);

    @Test
    void returnsFixedResponseRegardlessOfInput() {
        String first = client.generateJson("SYS", ExtractReturnReportPrompt.userMessage("아무 내용"), null);
        String second = client.generateJson("SYS", ExtractReturnReportPrompt.userMessage("다른 내용"), null);

        assertEquals(MockLlmClient.FIXED_RESPONSE, first);
        assertEquals(first, second);
        assertFalse(client.verifiesSource());
    }

    @Test
    void failsWhenTextStartsWithTrigger() {
        LlmException e = assertThrows(LlmException.class,
                () -> client.generateJson("SYS", ExtractReturnReportPrompt.userMessage("[MOCK_FAIL] 테스트"), null));

        assertEquals(LlmException.Kind.MOCK_FAILURE, e.getKind());
        assertFalse(e.getKind().isRetryable());
    }

    @Test
    void triggerInMiddleDoesNotFail() {
        assertEquals(MockLlmClient.FIXED_RESPONSE,
                client.generateJson("SYS", ExtractReturnReportPrompt.userMessage("본문 [MOCK_FAIL]"), null));
    }
}
