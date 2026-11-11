package com.likelion14th._Team.domain.studyplan.dto;

import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import com.likelion14th._Team.global.response.FieldErrorDetail;
import java.math.BigDecimal;
import java.util.List;
import tools.jackson.databind.JsonNode;

public record StudyPlanItemCreateRequest(
        String itemType, String courseName, BigDecimal foreignCredit,
        Long sourceCourseId, Long homeCourseId, String recognitionType
) {
    public static StudyPlanItemCreateRequest from(JsonNode body) {
        if (body == null || !body.isObject()) {
            throw invalid("body", "JSON 객체를 입력해 주세요.");
        }
        String type = text(body, "itemType", 20, true);
        String name = text(body, "courseName", 200, true);
        BigDecimal credit = credit(body);
        Long source = id(body, "sourceCourseId");
        Long home = id(body, "homeCourseId");
        String recognition = text(body, "recognitionType", 20, false);
        switch (type) {
            case "REGISTERED" -> {
                if (source == null) {
                    throw invalid("sourceCourseId", "등록 과목의 원본 과목 ID가 필요합니다.");
                }
            }
            case "CUSTOM" -> {
                if (source != null) {
                    throw invalid("sourceCourseId", "직접 입력 과목은 원본 과목을 연결할 수 없습니다.");
                }
            }
            default -> throw invalid("itemType", "REGISTERED 또는 CUSTOM을 입력해 주세요.");
        }
        return new StudyPlanItemCreateRequest(type, name, credit, source, home, recognition);
    }

    static BigDecimal credit(JsonNode body) {
        JsonNode creditNode = body.get("foreignCredit");
        if (creditNode == null || !creditNode.isNumber()) {
            throw invalid("foreignCredit", "외국 학점을 숫자로 입력해 주세요.");
        }
        BigDecimal credit = new BigDecimal(creditNode.toString());
        if (credit.signum() <= 0 || credit.compareTo(new BigDecimal("99.9")) > 0
                || credit.stripTrailingZeros().scale() > 1) {
            throw invalid("foreignCredit", "0보다 크고 99.9 이하인 소수점 한 자리 학점을 입력해 주세요.");
        }
        return credit;
    }

    private static String text(JsonNode body, String field, int maxLength, boolean required) {
        JsonNode node = body.get(field);
        if (!required && (node == null || node.isNull())) {
            return null;
        }
        if (node == null || !node.isString()) {
            throw invalid(field, "문자열을 입력해 주세요.");
        }
        String value = node.stringValue();
        if ((required && value.isBlank()) || value.codePointCount(0, value.length()) > maxLength) {
            throw invalid(field, "필수 여부와 최대 " + maxLength + "자 제한을 확인해 주세요.");
        }
        return value;
    }

    static Long id(JsonNode body, String field) {
        JsonNode node = body.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        if (!node.isIntegralNumber() || !node.canConvertToLong() || node.longValue() <= 0) {
            throw invalid(field, "양의 정수 ID를 입력해 주세요.");
        }
        return node.longValue();
    }

    static CustomException invalid(String field, String reason) {
        return new CustomException(ErrorCode.INVALID_INPUT, "입력값이 올바르지 않습니다.",
                List.of(new FieldErrorDetail(field, reason)));
    }
}
