package com.likelion14th._Team.domain.coursecase.entity;

import com.likelion14th._Team.domain.course.entity.HomeCourse;
import com.likelion14th._Team.domain.returnreport.entity.ReturnReportCourse;
import com.likelion14th._Team.global.entity.BaseEntity;
import com.likelion14th._Team.global.enums.CourseCategory;
import com.likelion14th._Team.global.enums.MajorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 귀국보고서 수강 교과목 한 건에 대한 본교 학점 인정 결과. 과목 하나당 1건 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "course_case", uniqueConstraints = @UniqueConstraint(
        name = "uk_course_case_return_report_course", columnNames = {"return_report_course_id"}))
public class CourseCase extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "return_report_course_id", nullable = false)
    private ReturnReportCourse returnReportCourse;

    /** 교양이면 null */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_course_id")
    private HomeCourse homeCourse;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "recognition_type", nullable = false, length = 20)
    private CourseCategory recognitionType;

    @Column(name = "recognized_credit", nullable = false, precision = 3, scale = 1)
    private BigDecimal recognizedCredit;

    /** 등록 시점의 내 전공 구분을 복사해 저장하고, 수정해도 바꾸지 않는다 */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "major_type", nullable = false, length = 20)
    private MajorType majorType;

    /** 대상 과목이 속한 귀국보고서의 학기. 예: 2026-2 */
    @Column(nullable = false, length = 10)
    private String semester;

    @Builder
    private CourseCase(ReturnReportCourse returnReportCourse, HomeCourse homeCourse,
                       CourseCategory recognitionType, BigDecimal recognizedCredit,
                       MajorType majorType, String semester) {
        this.returnReportCourse = returnReportCourse;
        this.homeCourse = homeCourse;
        this.recognitionType = recognitionType;
        this.recognizedCredit = recognizedCredit;
        this.majorType = majorType;
        this.semester = semester;
    }

    /** majorType은 유지하고, 대상 과목을 바꾸면 semester도 새 과목의 학기로 바뀐다 */
    public void update(ReturnReportCourse returnReportCourse, HomeCourse homeCourse,
                       CourseCategory recognitionType, BigDecimal recognizedCredit, String semester) {
        this.returnReportCourse = returnReportCourse;
        this.homeCourse = homeCourse;
        this.recognitionType = recognitionType;
        this.recognizedCredit = recognizedCredit;
        this.semester = semester;
    }
}