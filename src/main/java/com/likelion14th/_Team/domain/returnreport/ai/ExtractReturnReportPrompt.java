package com.likelion14th._Team.domain.returnreport.ai;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.core.io.ClassPathResource;

/**
 * 귀국보고서 추출 프롬프트(시스템 지시는 파일, 사용자 메시지는 report 태그로 감싼 원문)와 출력 스키마.
 * 프롬프트를 바꿀 때는 기존 파일을 고치지 않고 새 버전 파일(v2...)을 만든 뒤 ai.prompt.extract-return-report를 바꾼다.
 */
public class ExtractReturnReportPrompt {

    private static final String OPEN_TAG = "<report>";
    private static final String CLOSE_TAG = "</report>";
    /** 원문 안의 report 태그는 데이터 경계를 흐리므로 대괄호로 바꿔 보낸다 */
    private static final Pattern REPORT_TAG = Pattern.compile("(?i)</?report>");

    private final String version;
    private final String systemInstruction;

    public ExtractReturnReportPrompt(String version) {
        this.version = version;
        this.systemInstruction = load("prompts/extract_return_report_" + version + ".txt");
    }

    public String version() {
        return version;
    }

    public String systemInstruction() {
        return systemInstruction;
    }

    /** 원문 안의 태그를 무력화한 텍스트. 원문 포함 검증도 이 텍스트 기준으로 한다 */
    public static String neutralize(String maskedText) {
        return REPORT_TAG.matcher(maskedText).replaceAll(match -> match.group().startsWith("</") ? "[/report]" : "[report]");
    }

    public static String userMessage(String neutralizedText) {
        return OPEN_TAG + "\n" + neutralizedText + "\n" + CLOSE_TAG;
    }

    /** userMessage에서 원문 부분만 꺼낸다 (mock의 실패 트리거 확인용) */
    static String reportText(String userMessage) {
        String text = userMessage;
        if (text.startsWith(OPEN_TAG + "\n")) {
            text = text.substring(OPEN_TAG.length() + 1);
        }
        if (text.endsWith("\n" + CLOSE_TAG)) {
            text = text.substring(0, text.length() - CLOSE_TAG.length() - 1);
        }
        return text;
    }

    /** AI 내부 출력 스키마 (ai-spec 5장). API 응답 필드명으로의 변환은 후처리에서 한다 */
    public static Map<String, Object> responseSchema() {
        Map<String, Object> nullableString = Map.of("type", List.of("string", "null"));
        Map<String, Object> cost = Map.of(
                "type", "object",
                "properties", Map.of(
                        "airfare", nullableString,
                        "insurance", nullableString,
                        "monthlyLiving", nullableString,
                        "total", nullableString),
                "required", List.of("airfare", "insurance", "monthlyLiving", "total"));
        Map<String, Object> visa = Map.of(
                "type", "object",
                "properties", Map.of("duration", nullableString, "type", nullableString),
                "required", List.of("duration", "type"));
        Map<String, Object> course = Map.of(
                "type", "object",
                "properties", Map.of(
                        "code", nullableString,
                        "nameEn", Map.of("type", "string"),
                        "foreignCredit", Map.of("type", List.of("number", "null")),
                        "satisfaction", Map.of("type", List.of("integer", "null"), "minimum", 1, "maximum", 5)),
                "required", List.of("code", "nameEn", "foreignCredit", "satisfaction"));
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "cost", cost,
                        "housing", Map.of("type", List.of("string", "null"),
                                "description", "DORM(기숙사), SELF(자취), OTHER(기타) 중 하나. 알 수 없으면 null"),
                        "visa", visa,
                        "insuranceProvider", nullableString,
                        "courses", Map.of("type", "array", "items", course),
                        "summary", nullableString),
                "required", List.of("cost", "housing", "visa", "insuranceProvider", "courses", "summary"));
    }

    private static String load(String path) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).strip();
        } catch (IOException e) {
            throw new UncheckedIOException("프롬프트 파일을 읽을 수 없습니다: " + path, e);
        }
    }
}
