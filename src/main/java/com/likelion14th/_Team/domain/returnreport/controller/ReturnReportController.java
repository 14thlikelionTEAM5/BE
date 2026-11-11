package com.likelion14th._Team.domain.returnreport.controller;

import com.likelion14th._Team.domain.returnreport.dto.ReturnReportExtractRequest;
import com.likelion14th._Team.domain.returnreport.dto.ReturnReportExtractResponse;
import com.likelion14th._Team.domain.returnreport.dto.ReturnReportSaveRequest;
import com.likelion14th._Team.domain.returnreport.dto.ReturnReportSaveResponse;
import com.likelion14th._Team.domain.returnreport.dto.ReturnReportUpdateRequest;
import com.likelion14th._Team.domain.returnreport.service.ReturnReportExtractService;
import com.likelion14th._Team.domain.returnreport.service.ReturnReportService;
import com.likelion14th._Team.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "ReturnReport", description = "귀국보고서 API")
@RestController
@RequestMapping("/api/v1/return-reports")
@RequiredArgsConstructor
public class ReturnReportController {

    private final ReturnReportService returnReportService;
    private final ReturnReportExtractService returnReportExtractService;

    @Operation(summary = "귀국보고서 항목 추출",
            description = "붙여넣은 귀국보고서 텍스트(최대 20,000자)에서 비용·숙소·비자·보험·수강 교과목·요약을 추출합니다. "
                    + "결과만 반환하고 저장하지 않으며, 원문도 저장하지 않습니다. 찾지 못한 값은 null입니다.")
    @PostMapping("/extract")
    public ResponseEntity<ApiResponse<ReturnReportExtractResponse>> extract(
            @Valid @RequestBody ReturnReportExtractRequest request) {
        ReturnReportExtractResponse response = returnReportExtractService.extract(request);
        return ResponseEntity.ok(ApiResponse.ok("항목이 추출되었습니다.", response));
    }

    @Operation(summary = "귀국보고서 저장",
            description = "확인·수정을 마친 항목과 요약을 저장합니다. 한 학기에 1건만 등록할 수 있습니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<ReturnReportSaveResponse>> save(
            @AuthenticationPrincipal Long userId, @Valid @RequestBody ReturnReportSaveRequest request) {
        ReturnReportSaveResponse response = returnReportService.save(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("귀국보고서가 저장되었습니다.", response));
    }

    @Operation(summary = "귀국보고서 수정",
            description = "학교·학기를 제외한 항목 전체를 덮어씁니다. 수강 교과목은 courseId가 있으면 수정, 없으면 추가, "
                    + "보내지 않은 기존 행은 삭제합니다.")
    @PutMapping("/{reportId}")
    public ResponseEntity<ApiResponse<ReturnReportSaveResponse>> update(
            @AuthenticationPrincipal Long userId, @PathVariable Long reportId,
            @Valid @RequestBody ReturnReportUpdateRequest request) {
        ReturnReportSaveResponse response = returnReportService.update(userId, reportId, request);
        return ResponseEntity.ok(ApiResponse.ok("귀국보고서가 수정되었습니다.", response));
    }

    @Operation(summary = "귀국보고서 삭제",
            description = "귀국보고서와 수강 교과목을 삭제합니다. 이 과목을 담은 수강계획 항목은 남기고 원본 연결만 해제합니다.")
    @DeleteMapping("/{reportId}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal Long userId, @PathVariable Long reportId) {
        returnReportService.delete(userId, reportId);
        return ResponseEntity.ok(ApiResponse.ok("귀국보고서가 삭제되었습니다.", null));
    }
}
