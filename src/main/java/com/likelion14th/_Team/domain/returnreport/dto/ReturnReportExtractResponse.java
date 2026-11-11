package com.likelion14th._Team.domain.returnreport.dto;

import java.math.BigDecimal;
import java.util.List;

/** 추출 결과. 찾지 못한 값은 null, cost·visa는 항상 객체, 교과목이 없으면 빈 배열 */
public record ReturnReportExtractResponse(
        ReturnReportCost cost,
        String housingType,
        ReturnReportVisa visa,
        String insuranceProvider,
        List<ExtractedCourse> courses,
        String reviewSummary
) {

    public record ExtractedCourse(String courseCode, String courseNameEn, BigDecimal foreignCredit,
                                  Integer satisfaction) {
    }
}
