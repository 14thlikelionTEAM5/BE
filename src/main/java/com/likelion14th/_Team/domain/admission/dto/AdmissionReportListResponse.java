package com.likelion14th._Team.domain.admission.dto;

import java.math.BigDecimal;
import java.util.List;

public record AdmissionReportListResponse(Long universityId, int totalCount, List<Item> admissionReports) {
    public record Item(Long admissionReportId, String semester, String result, String majorType,
                       BigDecimal gpa, String languageTestType, BigDecimal languageScore) {}
}
