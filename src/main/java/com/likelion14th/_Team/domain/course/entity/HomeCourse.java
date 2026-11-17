package com.likelion14th._Team.domain.course.entity;

import com.likelion14th._Team.global.entity.BaseEntity;
import com.likelion14th._Team.global.enums.CourseCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 본교(한국외대) 경영대학 개설 과목.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "home_course")
public class HomeCourse extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** UNIQUE */
    @Column(name = "code", nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "credit", nullable = false, precision = 3, scale = 1)
    private BigDecimal credit;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "category", nullable = false, length = 20)
    private CourseCategory category;

    @Column(name = "is_business_offered", nullable = false)
    private boolean isBusinessOffered;

    /** 1~4. 학년 구분 없는 과목은 null */
    @Column(name = "recommended_grade")
    private Integer recommendedGrade;

    /** 교과과정표 기준 학년도. 예: 2025-2026 */
    @Column(name = "base_year", nullable = false, length = 9)
    private String baseYear;

    @Builder
    private HomeCourse(String code, String name, BigDecimal credit, CourseCategory category,
                       Boolean isBusinessOffered, Integer recommendedGrade, String baseYear) {
        this.code = code;
        this.name = name;
        this.credit = credit;
        this.category = category;
        this.isBusinessOffered = isBusinessOffered == null || isBusinessOffered;  // 기본값 true
        this.recommendedGrade = recommendedGrade;
        this.baseYear = baseYear;
    }
}
