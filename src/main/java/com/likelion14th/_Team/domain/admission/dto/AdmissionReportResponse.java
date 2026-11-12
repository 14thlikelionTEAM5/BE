package com.likelion14th._Team.domain.admission.dto;

import com.likelion14th._Team.domain.admission.entity.AdmissionReport;
import com.likelion14th._Team.global.enums.AdmissionResult;
import com.likelion14th._Team.global.enums.LanguageTestType;
import com.likelion14th._Team.global.enums.MajorType;
import java.math.BigDecimal;

/** 합격·탈락 스펙 저장·수정 응답 */
public record AdmissionReportResponse(
        Long admissionReportId,
        Long universityId,
        String semester,
        AdmissionResult result,
        LanguageTestType languageTestType,
        BigDecimal languageScore,
        BigDecimal gpa,
        MajorType majorType
) {

    public static AdmissionReportResponse from(AdmissionReport report) {
        return new AdmissionReportResponse(
                report.getId(),
                report.getUniversity().getId(),
                report.getSemester(),
                report.getResult(),
                report.getLanguageTestType(),
                report.getLanguageScore(),
                report.getGpa(),
                report.getMajorType());
    }
}