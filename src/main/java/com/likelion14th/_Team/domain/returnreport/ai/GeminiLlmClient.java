package com.likelion14th._Team.domain.returnreport.ai;

import java.net.SocketTimeoutException;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * AI_MODE=real. Gemini Interactions API(POST /v1beta/interactions)를 RestClient로 직접 호출한다.
 * - 키는 x-goog-api-key 헤더로만 보낸다 (URL 쿼리에 넣지 않음)
 * - store=false로 요청해 원문이 외부 서비스에 보관되지 않게 한다
 * - temperature는 Gemini 3.6+에서 무시되므로 보내지 않는다
 * - 재시도는 호출하는 쪽(ReturnReportExtractService)에서 실패 종류를 보고 결정한다
 */
public class GeminiLlmClient implements LlmClient {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final String API_KEY_HEADER = "x-goog-api-key";
    private static final String API_REVISION_HEADER = "Api-Revision";

    private final AiProperties.Gemini properties;
    private final RestClient restClient;

    public GeminiLlmClient(AiProperties.Gemini properties) {
        this(properties, RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory(properties))
                .build());
    }

    /** 테스트에서 MockRestServiceServer에 연결한 RestClient를 넣기 위한 생성자 */
    GeminiLlmClient(AiProperties.Gemini properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    private static JdkClientHttpRequestFactory requestFactory(AiProperties.Gemini properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.connectTimeoutSeconds()))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(properties.timeoutSeconds()));
        return factory;
    }

    @Override
    public String generateJson(String systemInstruction, String userMessage, Map<String, Object> responseSchema) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new LlmException(LlmException.Kind.MISSING_API_KEY);
        }

        String responseBody;
        try {
            responseBody = restClient.post()
                    .uri("/interactions")
                    .header(API_KEY_HEADER, properties.apiKey())
                    .header(API_REVISION_HEADER, properties.apiRevision())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(MAPPER.writeValueAsString(requestBody(systemInstruction, userMessage, responseSchema)))
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            throw new LlmException(kindOf(e.getStatusCode().value()), e.getStatusCode().value());
        } catch (ResourceAccessException e) {
            throw new LlmException(isTimeout(e) ? LlmException.Kind.TIMEOUT : LlmException.Kind.UNAVAILABLE);
        } catch (RestClientException e) {
            throw new LlmException(LlmException.Kind.UNAVAILABLE);
        }
        return outputText(responseBody);
    }

    Map<String, Object> requestBody(String systemInstruction, String userMessage, Map<String, Object> responseSchema) {
        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("max_output_tokens", properties.maxOutputTokens());
        generationConfig.put("thinking_level", properties.thinkingLevel());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.model());
        body.put("system_instruction", systemInstruction);
        body.put("input", userMessage);
        body.put("generation_config", generationConfig);
        body.put("response_format", Map.of(
                "type", "text",
                "mime_type", "application/json",
                "schema", responseSchema));
        body.put("store", false);
        return body;
    }

    /**
     * Interaction 응답에서 모델 출력 텍스트를 꺼낸다.
     * status=completed만 성공, incomplete(출력 상한 도달 등)는 파싱 실패와 같게 재시도 대상으로 본다.
     */
    static String outputText(String responseBody) {
        JsonNode root;
        try {
            root = MAPPER.readTree(responseBody == null ? "" : responseBody);
        } catch (JacksonException e) {
            throw new LlmException(LlmException.Kind.PARSE_FAILED);
        }
        if (root == null || !root.isObject()) {
            throw new LlmException(LlmException.Kind.PARSE_FAILED);
        }

        String status = root.path("status").isString() ? root.path("status").stringValue() : "";
        switch (status) {
            case "completed" -> {
            }
            case "incomplete" -> throw new LlmException(LlmException.Kind.INCOMPLETE);
            case "failed" -> throw new LlmException(LlmException.Kind.FAILED);
            default -> throw new LlmException(LlmException.Kind.UNEXPECTED_STATUS);
        }

        StringBuilder text = new StringBuilder();
        for (JsonNode step : root.path("steps")) {
            if (!"model_output".equals(step.path("type").asString(""))) {
                continue;
            }
            for (JsonNode content : step.path("content")) {
                if ("text".equals(content.path("type").asString("")) && content.path("text").isString()) {
                    text.append(content.path("text").stringValue());
                }
            }
        }
        if (text.isEmpty() || text.toString().isBlank()) {
            throw new LlmException(LlmException.Kind.EMPTY);
        }
        return text.toString();
    }

    private static LlmException.Kind kindOf(int status) {
        if (status == 401 || status == 403) {
            return LlmException.Kind.AUTH;
        }
        if (status == 429) {
            return LlmException.Kind.RATE_LIMITED;
        }
        if (status >= 500) {
            return LlmException.Kind.UNAVAILABLE;
        }
        return LlmException.Kind.CLIENT_ERROR;
    }

    private static boolean isTimeout(Throwable e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof HttpTimeoutException || cause instanceof SocketTimeoutException) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String describe() {
        return "real(" + properties.model() + ")";
    }
}
