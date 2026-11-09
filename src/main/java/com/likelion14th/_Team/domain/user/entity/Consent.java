package com.likelion14th._Team.domain.user.entity;

import com.likelion14th._Team.global.entity.BaseEntity;
import com.likelion14th._Team.global.enums.ConsentType;
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
import java.time.LocalDateTime;
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
        name = "consent",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_consent_user_type",
                columnNames = {"user_id", "consent_type"}))
public class Consent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "consent_type", nullable = false, length = 30)
    private ConsentType consentType;

    @Column(name = "consented_at", nullable = false)
    private LocalDateTime consentedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder
    private Consent(ConsentType consentType, LocalDateTime consentedAt, User user) {
        this.consentType = consentType;
        this.consentedAt = consentedAt;
        this.user = user;
    }
}
