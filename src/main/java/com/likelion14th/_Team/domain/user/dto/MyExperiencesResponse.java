package com.likelion14th._Team.domain.user.dto;

import com.likelion14th._Team.domain.admission.entity.AdmissionReport;
import com.likelion14th._Team.domain.course.entity.HomeCourse;
import com.likelion14th._Team.domain.coursecase.entity.CourseCase;
import com.likelion14th._Team.domain.returnreport.entity.ReturnReport;
import com.likelion14th._Team.domain.returnreport.entity.ReturnReportCourse;
import com.likelion14th._Team.global.enums.AdmissionResult;
import com.likelion14th._Team.global.enums.CourseCategory;
import com.likelion14th._Team.global.enums.LanguageTestType;
import com.likelion14th._Team.global.enums.MajorType;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/** 내 등록 내역 조회 응답. 내역이 없는 항목은 빈 배열 */
public record MyExperiencesResponse(
        List<AdmissionReportItem> admissionReports,
        List<ReturnReportItem> returnReports,
        List<CourseCaseItem> courseCases
) {

    public record AdmissionReportItem(
            Long admissionReportId,
            Long universityId,
            String universityName,
            String semester,
            AdmissionResult result,
            LanguageTestType languageTestType,
            BigDecimal languageScore,
            BigDecimal gpa,
            MajorType majorType
    ) {
        public static AdmissionReportItem from(AdmissionReport report) {
            return new AdmissionReportItem(
                    report.getId(),
                    report.getUniversity().getId(),
                    report.getUniversity().getName(),
                    report.getSemester(),
                    report.getResult(),
                    report.getLanguageTestType(),
                    report.getLanguageScore(),
                    report.getGpa(),
                    report.getMajorType());
        }
    }

    public record ReturnReportItem(
            Long returnReportId,
            Long universityId,
            String universityName,
            String semester,
            Cost cost,
            String housingType,
            Visa visa,
            String insuranceProvider,
            List<Course> courses,
            String reviewSummary
    ) {
        public record Cost(String flight, String insurance, String livingMonthly, String total) {
        }

        public record Visa(String duration, String type) {
        }

        /** satisfaction은 1~5 정수 */
        public record Course(Long courseId, String courseCode, String courseNameEn,
                             BigDecimal foreignCredit, Integer satisfaction) {
            public static Course from(ReturnReportCourse course) {
                return new Course(course.getId(), course.getCourseCode(), course.getCourseNameEn(),
                        course.getForeignCredit(), course.getSatisfaction());
            }
        }

        /** courses는 courseId 오름차순으로 내려준다 */
        public static ReturnReportItem of(ReturnReport report, List<ReturnReportCourse> courses) {
            List<Course> courseItems = courses.stream()
                    .sorted(Comparator.comparing(ReturnReportCourse::getId))
                    .map(Course::from)
                    .toList();
            return new ReturnReportItem(
                    report.getId(),
                    report.getUniversity().getId(),
                    report.getUniversity().getName(),
                    report.getSemester(),
                    new Cost(report.getCostFlight(), report.getCostInsurance(),
                            report.getCostLivingMonthly(), report.getCostTotal()),
                    report.getHousingType(),
                    new Visa(report.getVisaDuration(), report.getVisaType()),
                    report.getInsuranceProvider(),
                    courseItems,
                    report.getReviewSummary());
        }
    }

    public record CourseCaseItem(
            Long courseCaseId,
            Long universityId,
            String universityName,
            String semester,
            Long returnReportCourseId,
            String courseNameEn,
            CourseCategory recognitionType,
            HomeCourseSummary homeCourse,
            BigDecimal recognizedCredit,
            MajorType majorType
    ) {
        /** homeCourse는 교양이면 null */
        public record HomeCourseSummary(Long homeCourseId, String code, String name, BigDecimal credit) {
            static HomeCourseSummary from(HomeCourse course) {
                return new HomeCourseSummary(course.getId(), course.getCode(), course.getName(), course.getCredit());
            }
        }

        public static CourseCaseItem from(CourseCase courseCase) {
            ReturnReportCourse course = courseCase.getReturnReportCourse();
            ReturnReport report = course.getReturnReport();
            HomeCourse homeCourse = courseCase.getHomeCourse();
            return new CourseCaseItem(
                    courseCase.getId(),
                    report.getUniversity().getId(),
                    report.getUniversity().getName(),
                    courseCase.getSemester(),
                    course.getId(),
                    course.getCourseNameEn(),
                    courseCase.getRecognitionType(),
                    homeCourse == null ? null : HomeCourseSummary.from(homeCourse),
                    courseCase.getRecognizedCredit(),
                    courseCase.getMajorType());
        }
    }
}