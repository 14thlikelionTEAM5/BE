package com.likelion14th._Team.domain.university.dto;

import java.math.BigDecimal;
import java.util.List;

public record UniversityCoursesResponse(Long universityId, List<Course> courses) {
    public record Course(Long courseId, String courseCode, String courseName,
                         BigDecimal foreignCredit, boolean hasRecognition) {}
}
