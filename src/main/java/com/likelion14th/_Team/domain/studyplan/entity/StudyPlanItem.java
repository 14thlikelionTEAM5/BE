package com.likelion14th._Team.domain.studyplan.entity;

import com.likelion14th._Team.domain.course.entity.HomeCourse;
import com.likelion14th._Team.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "study_plan_item", uniqueConstraints = @UniqueConstraint(
        name = "uk_study_plan_item_source", columnNames = {"study_plan_id", "source_course_id"}))
public class StudyPlanItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "study_plan_id", nullable = false)
    private StudyPlan studyPlan;

    @Column(name = "item_type", nullable = false, length = 20)
    private String itemType;

    @Column(name = "course_name", nullable = false, length = 200)
    private String courseName;

    @Column(name = "foreign_credit", nullable = false, precision = 3, scale = 1)
    private BigDecimal foreignCredit;

    @Column(name = "source_course_id")
    private Long sourceCourseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_course_id")
    private HomeCourse homeCourse;

    @Column(name = "recognition_type", length = 20)
    private String recognitionType;

    public String getResponseItemType() {
        return switch (itemType) {
            case "CUSTOM" -> "DIRECT";
            case "REGISTERED" -> "REFERENCE";
            default -> itemType;
        };
    }

    @Builder
    private StudyPlanItem(StudyPlan studyPlan, String itemType, String courseName, BigDecimal foreignCredit,
                          Long sourceCourseId, HomeCourse homeCourse, String recognitionType) {
        this.studyPlan = studyPlan;
        this.itemType = itemType;
        this.courseName = courseName;
        this.foreignCredit = foreignCredit;
        this.sourceCourseId = sourceCourseId;
        this.homeCourse = homeCourse;
        this.recognitionType = recognitionType;
    }
}
