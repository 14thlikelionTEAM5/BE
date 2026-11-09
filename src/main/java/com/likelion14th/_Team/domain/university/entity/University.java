package com.likelion14th._Team.domain.university.entity;

import com.likelion14th._Team.global.entity.BaseEntity;
import com.likelion14th._Team.global.enums.CreditSystem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
@Table(
        name = "university",
        indexes = @Index(name = "idx_university_country", columnList = "country"))
public class University extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** UNIQUE */
    @Column(name = "name", nullable = false, unique = true, length = 200)
    private String name;

    @Column(name = "country", nullable = false, length = 100)
    private String country;

    /** true=확인된 인증, false=확인된 비인증, null=미확인 */
    @Column(name = "is_equis")
    private Boolean isEquis;

    /** true=확인된 인증, false=확인된 비인증, null=미확인 */
    @Column(name = "is_aacsb")
    private Boolean isAacsb;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "credit_system", nullable = false, length = 20)
    private CreditSystem creditSystem;

    @Column(name = "course_list_url", length = 500)
    private String courseListUrl;

    @Builder
    private University(String name, String country, Boolean isEquis, Boolean isAacsb,
                       CreditSystem creditSystem, String courseListUrl) {
        this.name = name;
        this.country = country;
        this.isEquis = isEquis;
        this.isAacsb = isAacsb;
        this.creditSystem = creditSystem != null ? creditSystem : CreditSystem.UNKNOWN;
        this.courseListUrl = courseListUrl;
    }
}
