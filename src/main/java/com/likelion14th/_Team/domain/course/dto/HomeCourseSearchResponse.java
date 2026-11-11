package com.likelion14th._Team.domain.course.dto;

import com.likelion14th._Team.domain.course.entity.HomeCourse;
import com.likelion14th._Team.global.enums.CourseCategory;
import java.math.BigDecimal;

public record HomeCourseSearchResponse(
        Long homeCourseId,
        String code,
        String name,
        BigDecimal credit,
        CourseCategory category
) {

    public static HomeCourseSearchResponse from(HomeCourse homeCourse) {
        return new HomeCourseSearchResponse(homeCourse.getId(), homeCourse.getCode(), homeCourse.getName(),
                homeCourse.getCredit(), homeCourse.getCategory());
    }
}
