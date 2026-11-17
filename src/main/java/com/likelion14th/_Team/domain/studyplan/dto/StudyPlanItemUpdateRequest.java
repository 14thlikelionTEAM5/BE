package com.likelion14th._Team.domain.studyplan.dto;

import java.math.BigDecimal;
import java.util.Set;
import tools.jackson.databind.JsonNode;

public record StudyPlanItemUpdateRequest(
        boolean changeForeignCredit, BigDecimal foreignCredit,
        boolean changeHomeCourse, Long homeCourseId
) {
    private static final Set<String> FIELDS = Set.of("foreignCredit", "homeCourseId");

    public static StudyPlanItemUpdateRequest from(JsonNode body) {
        if (body == null || !body.isObject() || body.isEmpty()) {
            throw StudyPlanItemCreateRequest.invalid("body", "수정할 필드를 하나 이상 입력해 주세요.");
        }
        for (String field : body.propertyNames()) {
            if (!FIELDS.contains(field)) {
                throw StudyPlanItemCreateRequest.invalid(field, "외국 학점과 본교 인정 과목만 수정할 수 있습니다.");
            }
        }
        boolean creditProvided = body.has("foreignCredit");
        BigDecimal credit = creditProvided ? StudyPlanItemCreateRequest.credit(body) : null;
        return new StudyPlanItemUpdateRequest(creditProvided, credit, body.has("homeCourseId"),
                StudyPlanItemCreateRequest.id(body, "homeCourseId"));
    }
}
