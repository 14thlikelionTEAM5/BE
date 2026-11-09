package com.likelion14th._Team.domain.user.entity;

import com.likelion14th._Team.global.entity.BaseEntity;
import com.likelion14th._Team.global.enums.MajorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "user")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 로그인 ID. UNIQUE */
    @Column(name = "school_email", nullable = false, unique = true, length = 100)
    private String schoolEmail;

    /** 해시 저장 */
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "major_type", nullable = false, length = 20)
    private MajorType majorType;

    /** UNIQUE (대소문자 구분 없음: DB collation utf8mb4_unicode_ci), 2~12자, 본인에게만 노출 */
    @Column(name = "nickname", nullable = false, unique = true, length = 12)
    private String nickname;

    @Builder
    private User(String schoolEmail, String password, MajorType majorType, String nickname) {
        this.schoolEmail = schoolEmail;
        this.password = password;
        this.majorType = majorType;
        this.nickname = nickname;
    }
}
