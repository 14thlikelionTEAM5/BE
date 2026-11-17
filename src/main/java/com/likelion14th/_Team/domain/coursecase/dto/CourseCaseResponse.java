package com.likelion14th._Team.domain.coursecase.dto;

import com.likelion14th._Team.domain.course.entity.HomeCourse;
import com.likelion14th._Team.domain.coursecase.entity.CourseCase;
import com.likelion14th._Team.global.enums.CourseCategory;
import com.likelion14th._Team.global.enums.MajorType;
import java.math.BigDecimal;

/** 저장·수정 응답. homeCourse는 교양이면 null */
public record CourseCaseResponse(
        Long courseCaseId,
        Long returnReportCourseId,
        String courseNameEn,
        CourseCategory recognitionType,
        HomeCourseSummary homeCourse,
        BigDecimal recognizedCredit,
        MajorType majorType,
        String semester
) {

    public record HomeCourseSummary(Long homeCourseId, String code, String name, BigDecimal credit) {

        static HomeCourseSummary from(HomeCourse course) {
            return new HomeCourseSummary(course.getId(), course.getCode(), course.getName(), course.getCredit());
        }
    }

    public static CourseCaseResponse of(CourseCase courseCase) {
        HomeCourse homeCourse = courseCase.getHomeCourse();
        return new CourseCaseResponse(
                courseCase.getId(),
                courseCase.getReturnReportCourse().getId(),
                courseCase.getReturnReportCourse().getCourseNameEn(),
                courseCase.getRecognitionType(),
                homeCourse == null ? null : HomeCourseSummary.from(homeCourse),
                courseCase.getRecognizedCredit(),
                courseCase.getMajorType(),
                courseCase.getSemester());
    }
}