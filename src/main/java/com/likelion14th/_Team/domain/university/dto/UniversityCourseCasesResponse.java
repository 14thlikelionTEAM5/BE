package com.likelion14th._Team.domain.university.dto;

import com.likelion14th._Team.domain.coursecase.entity.CourseCase;
import com.likelion14th._Team.global.enums.CourseCategory;
import com.likelion14th._Team.global.enums.MajorType;
import java.math.BigDecimal;
import java.util.List;

public record UniversityCourseCasesResponse(Long universityId, List<Item> courseCases) {
    public record Item(Long courseCaseId, String foreignCourseName, String homeCourseName,
            CourseCategory recognitionType, BigDecimal recognizedCredit, MajorType majorType, String semester) {
        public static Item from(CourseCase c) {
            String homeName = c.getRecognitionType() == CourseCategory.GENERAL || c.getHomeCourse() == null
                    ? null : c.getHomeCourse().getName();
            return new Item(c.getId(), c.getReturnReportCourse().getCourseNameEn(), homeName,
                    c.getRecognitionType(), c.getRecognizedCredit(), c.getMajorType(), c.getSemester());
        }
    }
}
