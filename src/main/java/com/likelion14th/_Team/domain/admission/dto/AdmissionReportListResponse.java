package com.likelion14th._Team.domain.admission.dto;

import java.math.BigDecimal;
import java.util.List;
import com.likelion14th._Team.global.enums.AdmissionResult;
import com.likelion14th._Team.global.enums.MajorType;
import com.likelion14th._Team.global.enums.LanguageTestType;

public record AdmissionReportListResponse(Long universityId, int totalCount, List<Item> admissionReports) {
    public record Item(Long admissionReportId, String semester, AdmissionResult result, MajorType majorType,
                       BigDecimal gpa, LanguageTestType languageTestType, BigDecimal languageScore) {}
}
