package com.likelion14th._Team.domain.returnreport.dto;

import com.likelion14th._Team.domain.returnreport.entity.ReturnReport;
import com.likelion14th._Team.domain.returnreport.entity.ReturnReportCourse;
import java.util.Comparator;
import java.util.List;

/** 저장·수정 응답. courses는 courseId 오름차순 */
public record ReturnReportSaveResponse(
        Long returnReportId,
        Long universityId,
        String semester,
        List<CourseSummary> courses
) {

    public record CourseSummary(Long courseId, String courseCode, String courseNameEn) {
    }

    public static ReturnReportSaveResponse of(ReturnReport report, List<ReturnReportCourse> courses) {
        List<CourseSummary> summaries = courses.stream()
                .sorted(Comparator.comparing(ReturnReportCourse::getId))
                .map(course -> new CourseSummary(course.getId(), course.getCourseCode(), course.getCourseNameEn()))
                .toList();
        return new ReturnReportSaveResponse(report.getId(), report.getUniversity().getId(), report.getSemester(),
                summaries);
    }
}
