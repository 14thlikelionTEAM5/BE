package com.likelion14th._Team.domain.returnreport.service;

import com.likelion14th._Team.domain.returnreport.ai.LlmException;
import com.likelion14th._Team.domain.returnreport.dto.ReturnReportCost;
import com.likelion14th._Team.domain.returnreport.dto.ReturnReportExtractResponse;
import com.likelion14th._Team.domain.returnreport.dto.ReturnReportExtractResponse.ExtractedCourse;
import com.likelion14th._Team.domain.returnreport.dto.ReturnReportVisa;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * AI 응답(내부 스키마) 검증 후 API 응답 필드로 변환한다 (ai-spec 6장 + 저장 API와 같은 제약).
 * 검증을 통과하지 못한 값은 자르지 않고 버린다(null). 모든 값이 비어도 오류가 아니다.
 * 원문 포함 검증은 요청 처리 중 메모리에서만 하며, 원문을 저장하거나 로그에 남기지 않는다.
 */
public class ExtractionPostProcessor {

    static final int COST_MAX = 200;
    static final int VISA_MAX = 100;
    static final int INSURANCE_PROVIDER_MAX = 200;
    static final int COURSE_CODE_MAX = 50;
    static final int COURSE_NAME_MAX = 200;
    static final int SUMMARY_MAX = 500;

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final List<String> REQUIRED_KEYS =
            List.of("cost", "housing", "visa", "insuranceProvider", "courses", "summary");
    private static final Set<String> HOUSING_TYPES = Set.of("DORM", "SELF", "OTHER");
    private static final BigDecimal MAX_CREDIT = new BigDecimal("99.9");
    /** 비교 전 정규화: 공백·문장부호·기호 제거 */
    private static final Pattern IGNORED = Pattern.compile("[\\s\\p{P}\\p{S}]+");
    private static final Pattern NUMBER = Pattern.compile("\\d+");

    public record Result(ReturnReportExtractResponse response, int droppedFields) {
    }

    /**
     * @param json   AI가 돌려준 JSON 문자열
     * @param source 마스킹된 입력 원문. null이면 원문 포함 검증을 건너뛴다 (mock)
     */
    public Result process(String json, String source) {
        JsonNode root = parse(json);
        Context context = new Context(source == null ? null : normalize(source));

        JsonNode cost = root.path("cost");
        ReturnReportCost costResult = new ReturnReportCost(
                context.text(cost.path("airfare"), COST_MAX),
                context.text(cost.path("insurance"), COST_MAX),
                context.text(cost.path("monthlyLiving"), COST_MAX),
                context.text(cost.path("total"), COST_MAX));

        JsonNode visa = root.path("visa");
        ReturnReportVisa visaResult = new ReturnReportVisa(
                context.text(visa.path("duration"), VISA_MAX),
                context.text(visa.path("type"), VISA_MAX));

        ReturnReportExtractResponse response = new ReturnReportExtractResponse(
                costResult,
                context.housing(root.path("housing")),
                visaResult,
                context.text(root.path("insuranceProvider"), INSURANCE_PROVIDER_MAX),
                context.courses(root.path("courses")),
                context.summary(root.path("summary")));
        return new Result(response, context.dropped);
    }

    private static JsonNode parse(String json) {
        JsonNode root;
        try {
            root = MAPPER.readTree(json == null ? "" : json);
        } catch (JacksonException e) {
            // 예외 메시지에 응답 본문 일부가 들어갈 수 있으므로 연결하지 않는다
            throw new LlmException(LlmException.Kind.PARSE_FAILED);
        }
        if (root == null || !root.isObject()) {
            throw new LlmException(LlmException.Kind.PARSE_FAILED);
        }
        for (String key : REQUIRED_KEYS) {
            if (!root.has(key)) {
                throw new LlmException(LlmException.Kind.PARSE_FAILED);
            }
        }
        return root;
    }

    static String normalize(String value) {
        return IGNORED.matcher(value).replaceAll("").toLowerCase(Locale.ROOT);
    }

    /** 요청 1건 동안의 검증 상태 */
    private static final class Context {

        private final String normalizedSource;
        private int dropped;

        private Context(String normalizedSource) {
            this.normalizedSource = normalizedSource;
        }

        /** 문자열 값: trim, 빈 값 null, 길이 초과·원문 미포함이면 버림 */
        private String text(JsonNode node, int maxLength) {
            String value = rawText(node);
            if (value == null) {
                return null;
            }
            if (value.length() > maxLength || !inSource(value)) {
                dropped++;
                return null;
            }
            return value;
        }

        private String housing(JsonNode node) {
            String value = rawText(node);
            if (value == null) {
                return null;
            }
            if (!HOUSING_TYPES.contains(value)) {
                dropped++;
                return null;
            }
            return value;
        }

        private List<ExtractedCourse> courses(JsonNode node) {
            List<ExtractedCourse> courses = new ArrayList<>();
            if (!node.isArray()) {
                if (!node.isNull() && !node.isMissingNode()) {
                    dropped++;
                }
                return courses;
            }
            for (JsonNode course : node) {
                if (!course.isObject()) {
                    dropped++;
                    continue;
                }
                // 영문 교과목명이 없거나 검증에서 빠지면 그 행 자체를 버린다
                JsonNode nameNode = course.path("nameEn");
                String nameEn = text(nameNode, COURSE_NAME_MAX);
                if (nameEn == null) {
                    // 문자열이 아니거나 검증 탈락은 text()에서 이미 셌다. 비어 있어서 버리는 행만 여기서 센다
                    if (nameNode.isNull() || nameNode.isMissingNode()
                            || (nameNode.isString() && nameNode.stringValue().isBlank())) {
                        dropped++;
                    }
                    continue;
                }
                courses.add(new ExtractedCourse(
                        text(course.path("code"), COURSE_CODE_MAX),
                        nameEn,
                        credit(course.path("foreignCredit")),
                        satisfaction(course.path("satisfaction"))));
            }
            return courses;
        }

        /** 0 초과 99.9 이하, 소수 첫째 자리까지 */
        private BigDecimal credit(JsonNode node) {
            if (node.isNull() || node.isMissingNode()) {
                return null;
            }
            if (!node.isNumber()) {
                dropped++;
                return null;
            }
            BigDecimal credit = node.decimalValue();
            if (credit.signum() <= 0 || credit.compareTo(MAX_CREDIT) > 0
                    || credit.stripTrailingZeros().scale() > 1) {
                dropped++;
                return null;
            }
            return credit.setScale(1, java.math.RoundingMode.UNNECESSARY);
        }

        /** 정수 1~5 */
        private Integer satisfaction(JsonNode node) {
            if (node.isNull() || node.isMissingNode()) {
                return null;
            }
            if (!node.isNumber()) {
                dropped++;
                return null;
            }
            BigDecimal value = node.decimalValue();
            if (value.stripTrailingZeros().scale() > 0 || value.compareTo(BigDecimal.ONE) < 0
                    || value.compareTo(BigDecimal.valueOf(5)) > 0) {
                dropped++;
                return null;
            }
            return value.intValue();
        }

        /** 길이 초과, 전화번호 포함, 원문에 없는 숫자 포함 중 하나라도 해당하면 요약만 비운다 */
        private String summary(JsonNode node) {
            String value = rawText(node);
            if (value == null) {
                return null;
            }
            if (value.length() > SUMMARY_MAX || PhoneNumberMasker.containsPhoneNumber(value)
                    || hasNumberNotInSource(value)) {
                dropped++;
                return null;
            }
            return value;
        }

        private boolean hasNumberNotInSource(String summary) {
            if (normalizedSource == null) {
                return false;
            }
            Matcher matcher = NUMBER.matcher(summary);
            while (matcher.find()) {
                if (!normalizedSource.contains(matcher.group())) {
                    return true;
                }
            }
            return false;
        }

        private boolean inSource(String value) {
            if (normalizedSource == null) {
                return true;
            }
            String normalized = normalize(value);
            return !normalized.isEmpty() && normalizedSource.contains(normalized);
        }

        /** 문자열이 아니면 버림, 공백만 있으면 null */
        private String rawText(JsonNode node) {
            if (node.isNull() || node.isMissingNode()) {
                return null;
            }
            if (!node.isString()) {
                dropped++;
                return null;
            }
            String trimmed = node.stringValue().trim();
            return trimmed.isEmpty() ? null : trimmed;
        }
    }
}
