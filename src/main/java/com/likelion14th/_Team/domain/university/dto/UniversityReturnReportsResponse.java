package com.likelion14th._Team.domain.university.dto;

import com.likelion14th._Team.domain.returnreport.entity.ReturnReport;
import java.util.List;

public record UniversityReturnReportsResponse(Long universityId, int totalCount, List<Report> returnReports) {
    public record Report(Long returnReportId, String semester, String costFlight, String costInsurance,
            String costLivingMonthly, String costTotal, String housingType, String visaDuration,
            String visaType, String insuranceProvider, String reviewSummary) {
        public static Report from(ReturnReport report) {
            return new Report(report.getId(), report.getSemester(), report.getCostFlight(),
                    report.getCostInsurance(), report.getCostLivingMonthly(), report.getCostTotal(),
                    report.getHousingType(), report.getVisaDuration(), report.getVisaType(),
                    report.getInsuranceProvider(), report.getReviewSummary());
        }
    }
}
