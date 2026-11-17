package com.likelion14th._Team.domain.course.entity;

import com.likelion14th._Team.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 학과(대학)별 학점인정 내규.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "credit_policy",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_credit_policy_department_effective",
                columnNames = {"department_name", "effective_from"}))
public class CreditPolicy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "department_name", nullable = false, length = 100)
    private String departmentName;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    /** 기본값 3 */
    @Column(name = "max_credit_per_course", nullable = false)
    private int maxCreditPerCourse;

    /** 기본값 18 */
    @Column(name = "max_credit_per_semester", nullable = false)
    private int maxCreditPerSemester;

    /** 기본값 0.6 */
    @Column(name = "ects_rate", nullable = false, precision = 3, scale = 2)
    private BigDecimal ectsRate;

    /** 기본값 false */
    @Column(name = "allow_major_required", nullable = false)
    private boolean allowMajorRequired;

    /** 기본값 true */
    @Column(name = "allow_major_required_minor", nullable = false)
    private boolean allowMajorRequiredMinor;

    /** 기본값 true */
    @Column(name = "only_business_courses", nullable = false)
    private boolean onlyBusinessCourses;

    /** 기본값 2024-03-21 */
    @Column(name = "standard_date", nullable = false)
    private LocalDate standardDate;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Builder
    private CreditPolicy(String departmentName, LocalDate effectiveFrom,
                         Integer maxCreditPerCourse, Integer maxCreditPerSemester,
                         BigDecimal ectsRate, Boolean allowMajorRequired,
                         Boolean allowMajorRequiredMinor, Boolean onlyBusinessCourses,
                         LocalDate standardDate, String sourceUrl) {
        this.departmentName = departmentName;
        this.effectiveFrom = effectiveFrom;
        this.maxCreditPerCourse = maxCreditPerCourse != null ? maxCreditPerCourse : 3;
        this.maxCreditPerSemester = maxCreditPerSemester != null ? maxCreditPerSemester : 18;
        this.ectsRate = ectsRate != null ? ectsRate : new BigDecimal("0.60");
        this.allowMajorRequired = allowMajorRequired != null && allowMajorRequired;
        this.allowMajorRequiredMinor = allowMajorRequiredMinor == null || allowMajorRequiredMinor;
        this.onlyBusinessCourses = onlyBusinessCourses == null || onlyBusinessCourses;
        this.standardDate = standardDate != null ? standardDate : LocalDate.of(2024, 3, 21);
        this.sourceUrl = sourceUrl;
    }
}
