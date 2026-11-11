package com.likelion14th._Team.domain.returnreport.entity;

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
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "return_report_course")
public class ReturnReportCourse extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "return_report_id", nullable = false)
    private ReturnReport returnReport;

    @Column(name = "course_code", length = 50)
    private String courseCode;

    @Column(name = "course_name_en", nullable = false, length = 200)
    private String courseNameEn;

    @Column(name = "foreign_credit", nullable = false, precision = 3, scale = 1)
    private BigDecimal foreignCredit;

    @Column
    private Integer satisfaction;
}
