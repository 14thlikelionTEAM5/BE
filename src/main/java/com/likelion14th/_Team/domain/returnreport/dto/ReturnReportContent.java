package com.likelion14th._Team.domain.returnreport.dto;

import java.util.List;

/** 저장·수정 요청이 공통으로 가지는 항목 */
public interface ReturnReportContent {

    ReturnReportCost cost();

    String housingType();

    ReturnReportVisa visa();

    String insuranceProvider();

    List<ReturnReportCourseRequest> courses();

    String reviewSummary();
}
