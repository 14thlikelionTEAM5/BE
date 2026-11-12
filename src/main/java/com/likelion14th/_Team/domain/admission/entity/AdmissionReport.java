package com.likelion14th._Team.domain.admission.entity;

import com.likelion14th._Team.global.entity.BaseEntity;
import com.likelion14th._Team.domain.university.entity.University;
import com.likelion14th._Team.domain.user.entity.User;
import jakarta.persistence.*;
import com.likelion14th._Team.global.enums.AdmissionResult;
import com.likelion14th._Team.global.enums.MajorType;
import com.likelion14th._Team.global.enums.LanguageTestType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "admission_report", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "university_id", "semester", "result"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdmissionReport extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "university_id", nullable = false)
    private University university;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(nullable = false, length = 10) private String semester;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10) private AdmissionResult result;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "language_test_type", length = 20) private LanguageTestType languageTestType;
    @Column(name = "language_score", precision = 5, scale = 1) private BigDecimal languageScore;
    @Column(nullable = false, precision = 3, scale = 2) private BigDecimal gpa;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "major_type", nullable = false, length = 20) private MajorType majorType;

    @Builder
    private AdmissionReport(University university, User user, String semester, AdmissionResult result,
                            LanguageTestType languageTestType, BigDecimal languageScore,
                            BigDecimal gpa, MajorType majorType) {
        this.university = university;
        this.user = user;
        this.semester = semester;
        this.result = result;
        this.languageTestType = languageTestType;
        this.languageScore = languageScore;
        this.gpa = gpa;
        this.majorType = majorType;
    }

    /** 학교·학기·결과·어학 성적만 덮어쓴다. gpa·majorType은 등록 시점 값을 유지한다 */
    public void update(University university, String semester, AdmissionResult result,
                       LanguageTestType languageTestType, BigDecimal languageScore) {
        this.university = university;
        this.semester = semester;
        this.result = result;
        this.languageTestType = languageTestType;
        this.languageScore = languageScore;
    }
}