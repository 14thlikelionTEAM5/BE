package com.likelion14th._Team.domain.university.entity;

import com.likelion14th._Team.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 학기별 협정 정보 (엑셀 import 기준).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "agreement",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_agreement_university_semester",
                columnNames = {"university_id", "semester"}),
        indexes = @Index(name = "idx_agreement_semester", columnList = "semester"))
public class Agreement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 예: 2027-1 */
    @Column(name = "semester", nullable = false, length = 10)
    private String semester;

    /** 미정이면 null */
    @Column(name = "quota")
    private Integer quota;

    @Column(name = "exchange_period", length = 50)
    private String exchangePeriod;

    /** null이면 "확인 필요" */
    @Column(name = "min_gpa", precision = 3, scale = 2)
    private BigDecimal minGpa;

    /** 엑셀 원문 그대로 */
    @Column(name = "cost_summary", columnDefinition = "TEXT")
    private String costSummary;

    @Column(name = "benefit_summary", columnDefinition = "TEXT")
    private String benefitSummary;

    @Column(name = "remark_summary", columnDefinition = "TEXT")
    private String remarkSummary;

    @Column(name = "course_restriction", columnDefinition = "TEXT")
    private String courseRestriction;

    /** 구분 "교환(경영)" */
    @Column(name = "is_business_only", nullable = false)
    private boolean isBusinessOnly;

    @Column(name = "is_open", nullable = false)
    private boolean isOpen;

    @Column(name = "announced_at", nullable = false)
    private LocalDate announcedAt;

    /** 엑셀 import 시각 */
    @Column(name = "imported_at", nullable = false)
    private LocalDateTime importedAt;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "university_id", nullable = false)
    private University university;

    @Builder
    private Agreement(String semester, Integer quota, String exchangePeriod, BigDecimal minGpa,
                      String costSummary, String benefitSummary, String remarkSummary,
                      String courseRestriction, Boolean isBusinessOnly, Boolean isOpen,
                      LocalDate announcedAt, LocalDateTime importedAt, String sourceUrl,
                      University university) {
        this.semester = semester;
        this.quota = quota;
        this.exchangePeriod = exchangePeriod;
        this.minGpa = minGpa;
        this.costSummary = costSummary;
        this.benefitSummary = benefitSummary;
        this.remarkSummary = remarkSummary;
        this.courseRestriction = courseRestriction;
        this.isBusinessOnly = isBusinessOnly != null && isBusinessOnly;   // 기본값 false
        this.isOpen = isOpen == null || isOpen;                           // 기본값 true
        this.announcedAt = announcedAt;
        this.importedAt = importedAt;
        this.sourceUrl = sourceUrl;
        this.university = university;
    }
}
