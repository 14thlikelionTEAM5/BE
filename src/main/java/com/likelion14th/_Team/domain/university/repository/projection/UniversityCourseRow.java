package com.likelion14th._Team.domain.university.repository.projection;

import java.math.BigDecimal;

/** 과목 조회 전용 Projection. API 응답 DTO와 독립적으로 사용한다. */
public record UniversityCourseRow(Long courseId, String courseCode, String courseName,
                                  BigDecimal foreignCredit, boolean hasRecognition) {
}
