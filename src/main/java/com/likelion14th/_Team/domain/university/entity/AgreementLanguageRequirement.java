package com.likelion14th._Team.domain.university.entity;

import com.likelion14th._Team.global.entity.BaseEntity;
import com.likelion14th._Team.global.enums.LanguageTestType;
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

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "agreement_language_requirement",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_agreement_language_requirement_agreement_test",
                columnNames = {"agreement_id", "test_type"}))
public class AgreementLanguageRequirement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "test_type", nullable = false, length = 20)
    private LanguageTestType testType;

    @Column(name = "min_score", nullable = false, precision = 5, scale = 1)
    private BigDecimal minScore;

    /** "영역별 N이상" */
    @Column(name = "min_section_score", precision = 3, scale = 1)
    private BigDecimal minSectionScore;

    /** 예: R:22 W:22 */
    @Column(name = "condition_note", length = 100)
    private String conditionNote;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agreement_id", nullable = false)
    private Agreement agreement;

    @Builder
    private AgreementLanguageRequirement(LanguageTestType testType, BigDecimal minScore,
                                         BigDecimal minSectionScore, String conditionNote,
                                         Agreement agreement) {
        this.testType = testType;
        this.minScore = minScore;
        this.minSectionScore = minSectionScore;
        this.conditionNote = conditionNote;
        this.agreement = agreement;
    }
}
