package com.likelion14th._Team.domain.returnreport.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.SocketTimeoutException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GeminiLlmClientTests {

    private static final String BASE_URL = "https://gemini.test/v1beta";
    private static final Map<String, Object> SCHEMA = Map.of("type", "object");

    private MockRestServiceServer server;
    private GeminiLlmClient client;

    @BeforeEach
    void setUp() {
        client = clientWithKey("test-key");
    }

    private GeminiLlmClient clientWithKey(String key) {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        AiProperties.Gemini properties = new AiProperties.Gemini(key, "gemini-3.8-flash", BASE_URL, "2026-05-20",
                5, 30, 45, "low", 8192);
        return new GeminiLlmClient(properties, builder.build());
    }

    private static String completed(String text) {
        return """
                {"id":"int_1","status":"completed","steps":[
                  {"type":"thought","signature":"sig","summary":[{"type":"text","text":"생각 요약"}]},
                  {"type":"model_output","content":[{"type":"text","text":%s}]}
                ]}
                """.formatted(text == null ? "null" : "\"" + text.replace("\"", "\\\"") + "\"");
    }

    @Test
    void sendsInteractionRequestWithHeaderKeyAndNoStore() {
        // URL 전체가 일치해야 하므로 쿼리(?key=)로 키가 붙지 않는 것도 함께 확인된다
        server.expect(requestTo(BASE_URL + "/interactions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "test-key"))
                .andExpect(header("Api-Revision", "2026-05-20"))
                .andExpect(jsonPath("$.model").value("gemini-3.8-flash"))
                .andExpect(jsonPath("$.system_instruction").value("SYS"))
                .andExpect(jsonPath("$.input").value("USER"))
                .andExpect(jsonPath("$.generation_config.max_output_tokens").value(8192))
                .andExpect(jsonPath("$.generation_config.thinking_level").value("low"))
                .andExpect(jsonPath("$.generation_config.temperature").doesNotExist())
                .andExpect(jsonPath("$.temperature").doesNotExist())
                .andExpect(jsonPath("$.response_format.type").value("text"))
                .andExpect(jsonPath("$.response_format.mime_type").value("application/json"))
                .andExpect(jsonPath("$.response_format.schema.type").value("object"))
                .andExpect(jsonPath("$.store").value(false))
                .andRespond(withSuccess(completed("{\"a\":1}"), MediaType.APPLICATION_JSON));

        assertEquals("{\"a\":1}", client.generateJson("SYS", "USER", SCHEMA));
        server.verify();
    }

    @Test
    void concatenatesOnlyModelOutputText() {
        server.expect(requestTo(BASE_URL + "/interactions")).andRespond(withSuccess("""
                {"status":"completed","steps":[
                  {"type":"model_output","content":[{"type":"text","text":"{\\"a\\":"},{"type":"text","text":"1}"}]}
                ]}""", MediaType.APPLICATION_JSON));

        assertEquals("{\"a\":1}", client.generateJson("SYS", "USER", SCHEMA));
    }

    @Test
    void incompleteIsRetryableLikeParseFailure() {
        server.expect(requestTo(BASE_URL + "/interactions"))
                .andRespond(withSuccess("{\"status\":\"incomplete\",\"steps\":[]}", MediaType.APPLICATION_JSON));

        LlmException e = assertThrows(LlmException.class, () -> client.generateJson("SYS", "USER", SCHEMA));

        assertEquals(LlmException.Kind.INCOMPLETE, e.getKind());
        assertEquals(true, e.getKind().isRetryable());
    }

    @Test
    void failedStatusIsNotRetryable() {
        server.expect(requestTo(BASE_URL + "/interactions"))
                .andRespond(withSuccess("{\"status\":\"failed\",\"steps\":[]}", MediaType.APPLICATION_JSON));

        LlmException e = assertThrows(LlmException.class, () -> client.generateJson("SYS", "USER", SCHEMA));

        assertEquals(LlmException.Kind.FAILED, e.getKind());
        assertFalse(e.getKind().isRetryable());
    }

    @Test
    void completedWithoutModelOutputIsEmpty() {
        server.expect(requestTo(BASE_URL + "/interactions"))
                .andRespond(withSuccess(completed(" "), MediaType.APPLICATION_JSON));

        assertEquals(LlmException.Kind.EMPTY,
                assertThrows(LlmException.class, () -> client.generateJson("SYS", "USER", SCHEMA)).getKind());
    }

    @Test
    void nonJsonEnvelopeIsParseFailure() {
        server.expect(requestTo(BASE_URL + "/interactions"))
                .andRespond(withSuccess("<html>oops</html>", MediaType.TEXT_HTML));

        assertEquals(LlmException.Kind.PARSE_FAILED,
                assertThrows(LlmException.class, () -> client.generateJson("SYS", "USER", SCHEMA)).getKind());
    }

    @Test
    void mapsHttpStatusToKindWithoutBodyInMessage() {
        String secretBody = "{\"error\":\"원문 일부가 섞인 응답\"}";
        Object[][] cases = {
                {HttpStatus.UNAUTHORIZED, LlmException.Kind.AUTH},
                {HttpStatus.FORBIDDEN, LlmException.Kind.AUTH},
                {HttpStatus.TOO_MANY_REQUESTS, LlmException.Kind.RATE_LIMITED},
                {HttpStatus.BAD_REQUEST, LlmException.Kind.CLIENT_ERROR},
                {HttpStatus.SERVICE_UNAVAILABLE, LlmException.Kind.UNAVAILABLE}};
        for (Object[] c : cases) {
            client = clientWithKey("test-key");
            server.expect(requestTo(BASE_URL + "/interactions"))
                    .andRespond(withStatus((HttpStatus) c[0]).body(secretBody).contentType(MediaType.APPLICATION_JSON));

            LlmException e = assertThrows(LlmException.class, () -> client.generateJson("SYS", "USER", SCHEMA));

            assertEquals(c[1], e.getKind());
            assertEquals(((HttpStatus) c[0]).value(), e.getHttpStatus());
            assertFalse(e.getMessage().contains("원문"));
            assertNull(e.getCause());
        }
    }

    @Test
    void serverErrorIsRetryable() {
        server.expect(requestTo(BASE_URL + "/interactions")).andRespond(withServerError());

        LlmException e = assertThrows(LlmException.class, () -> client.generateJson("SYS", "USER", SCHEMA));

        assertEquals(LlmException.Kind.UNAVAILABLE, e.getKind());
        assertEquals(true, e.getKind().isRetryable());
    }

    @Test
    void timeoutIsNotRetryable() {
        server.expect(requestTo(BASE_URL + "/interactions"))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));

        LlmException e = assertThrows(LlmException.class, () -> client.generateJson("SYS", "USER", SCHEMA));

        assertEquals(LlmException.Kind.TIMEOUT, e.getKind());
        assertFalse(e.getKind().isRetryable());
    }

    @Test
    void missingApiKeyFailsWithoutCallingApi() {
        for (String key : new String[]{null, "", "  "}) {
            GeminiLlmClient noKey = clientWithKey(key);

            LlmException e = assertThrows(LlmException.class, () -> noKey.generateJson("SYS", "USER", SCHEMA));

            assertEquals(LlmException.Kind.MISSING_API_KEY, e.getKind());
            server.verify();
        }
    }

    @Test
    void propertiesToStringHidesApiKey() {
        AiProperties.Gemini properties = new AiProperties.Gemini("super-secret", "m", BASE_URL, "r", 5, 30, 45, "low", 1);

        assertFalse(properties.toString().contains("super-secret"));
    }
}
