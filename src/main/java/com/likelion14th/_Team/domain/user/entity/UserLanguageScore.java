package com.likelion14th._Team.domain.user.entity;

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
        name = "user_language_score",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_language_score_user_test",
                columnNames = {"user_id", "test_type"}))
public class UserLanguageScore extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "test_type", nullable = false, length = 20)
    private LanguageTestType testType;

    @Column(name = "score", nullable = false, precision = 5, scale = 1)
    private BigDecimal score;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder
    private UserLanguageScore(LanguageTestType testType, BigDecimal score, User user) {
        this.testType = testType;
        this.score = score;
        this.user = user;
    }
}
