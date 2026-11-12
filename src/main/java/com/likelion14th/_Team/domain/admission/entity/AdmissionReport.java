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
}
