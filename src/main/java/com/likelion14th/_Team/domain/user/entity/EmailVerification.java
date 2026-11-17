package com.likelion14th._Team.domain.user.entity;

import com.likelion14th._Team.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 가입 전 학교 메일 인증용. 가입이 끝나면 역할이 끝난다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "email_verification")
public class EmailVerification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** UNIQUE */
    @Column(name = "school_email", nullable = false, unique = true, length = 100)
    private String schoolEmail;

    /** 해시 저장 */
    @Column(name = "code_hash", nullable = false, length = 255)
    private String codeHash;

    /** 유효 시간이 지나면 재요청 */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** NULL이면 미인증 */
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    /** 재발송 가능 시점 계산용 */
    @Column(name = "last_sent_at", nullable = false)
    private LocalDateTime lastSentAt;

    @Builder
    private EmailVerification(String schoolEmail, String codeHash, LocalDateTime expiresAt,
                              LocalDateTime verifiedAt, LocalDateTime lastSentAt) {
        this.schoolEmail = schoolEmail;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.verifiedAt = verifiedAt;
        this.lastSentAt = lastSentAt;
    }
    /** 새 코드로 교체한다. 이전 코드는 무효화되고 미인증 상태로 돌아간다 */
    public void renew(String codeHash, LocalDateTime expiresAt, LocalDateTime sentAt) {
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.lastSentAt = sentAt;
        this.verifiedAt = null;
    }

    /** 인증 완료 시각을 기록한다 */
    public void markVerified(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    /** 코드를 즉시 만료시킨다 (실패 횟수 초과 등) */
    public void expire(LocalDateTime now) {
        this.expiresAt = now;
    }
}
