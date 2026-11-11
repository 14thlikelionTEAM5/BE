package com.likelion14th._Team.domain.returnreport.entity;

import com.likelion14th._Team.domain.university.entity.University;
import com.likelion14th._Team.domain.user.entity.User;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 원본 과목의 소속 대학을 조회하는 귀국보고서. PassHUFS.sql의 기존 테이블 구조를 따른다. */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "return_report", uniqueConstraints = @UniqueConstraint(
        name = "uk_return_report_user_semester", columnNames = {"user_id", "semester"}))
public class ReturnReport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "university_id", nullable = false)
    private University university;

    @Column(nullable = false, length = 10)
    private String semester;

    @Column(name = "cost_flight", length = 200)
    private String costFlight;

    @Column(name = "cost_insurance", length = 200)
    private String costInsurance;

    @Column(name = "cost_living_monthly", length = 200)
    private String costLivingMonthly;

    @Column(name = "cost_total", length = 200)
    private String costTotal;

    @Column(name = "housing_type", length = 20)
    private String housingType;

    @Column(name = "visa_duration", length = 100)
    private String visaDuration;

    @Column(name = "visa_type", length = 100)
    private String visaType;

    @Column(name = "insurance_provider", length = 200)
    private String insuranceProvider;

    @Column(name = "review_summary", columnDefinition = "TEXT")
    private String reviewSummary;
}
