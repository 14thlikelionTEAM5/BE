package com.likelion14th._Team.domain.bookmark.dto;

import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import com.likelion14th._Team.global.response.FieldErrorDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import tools.jackson.databind.JsonNode;

public record BookmarkCreateRequest(
        @Schema(description = "찜할 학교 ID", example = "4", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Long universityId) {
    public static BookmarkCreateRequest from(JsonNode body) {
        if (body == null || !body.isObject()) {
            throw invalid("body", "JSON 객체를 입력해 주세요.");
        }
        // Long 자동 변환은 "4", 4.0, 4.9를 허용/절삭할 수 있으므로 JSON 정수 자체를 검증한다.
        JsonNode id = body.get("universityId");
        if (id == null || !id.isIntegralNumber() || !id.canConvertToLong() || id.longValue() <= 0) {
            throw invalid("universityId", "양의 정수 학교 ID를 입력해 주세요.");
        }
        return new BookmarkCreateRequest(id.longValue());
    }

    public static CustomException invalid(String field, String reason) {
        return new CustomException(ErrorCode.INVALID_INPUT, ErrorCode.INVALID_INPUT.getMessage(),
                List.of(new FieldErrorDetail(field, reason)));
    }
}
