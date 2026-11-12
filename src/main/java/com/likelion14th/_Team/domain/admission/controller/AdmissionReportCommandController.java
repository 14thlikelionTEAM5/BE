package com.likelion14th._Team.domain.admission.controller;

import com.likelion14th._Team.domain.admission.dto.AdmissionReportRequest;
import com.likelion14th._Team.domain.admission.dto.AdmissionReportResponse;
import com.likelion14th._Team.domain.admission.service.AdmissionReportService;
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

@Tag(name = "AdmissionReport", description = "합격·탈락 스펙 등록 API")
@RestController
@RequestMapping("/api/v1/admission-reports")
@RequiredArgsConstructor
public class AdmissionReportCommandController {

    private final AdmissionReportService admissionReportService;

    @Operation(summary = "합격·탈락 스펙 저장",
            description = "합격 또는 탈락한 지원 스펙을 등록합니다. 같은 학교·학기에 같은 유형(합격/탈락)은 1건만 등록할 수 있습니다. "
                    + "누적 학점과 전공 구분은 내 프로필에서 서버가 복사해 저장하며, 어학 성적이 없으면 languageTestType, languageScore를 둘 다 null로 보냅니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<AdmissionReportResponse>> save(
            @AuthenticationPrincipal Long userId, @Valid @RequestBody AdmissionReportRequest request) {
        AdmissionReportResponse response = admissionReportService.save(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("합격·탈락 스펙이 등록되었습니다.", response));
    }

    @Operation(summary = "합격·탈락 스펙 수정",
            description = "본인이 등록한 스펙을 수정합니다. 바꾸지 않을 항목도 기존 값을 그대로 보내고, "
                    + "어학 성적을 지우려면 languageTestType, languageScore를 둘 다 null로 보냅니다. "
                    + "등록 시점에 복사된 누적 학점과 전공 구분은 유지됩니다.")
    @PutMapping("/{reportId}")
    public ResponseEntity<ApiResponse<AdmissionReportResponse>> update(
            @AuthenticationPrincipal Long userId, @PathVariable Long reportId,
            @Valid @RequestBody AdmissionReportRequest request) {
        AdmissionReportResponse response = admissionReportService.update(userId, reportId, request);
        return ResponseEntity.ok(ApiResponse.ok("합격·탈락 스펙이 수정되었습니다.", response));
    }

    @Operation(summary = "합격·탈락 스펙 삭제", description = "본인이 등록한 스펙을 삭제합니다.")
    @DeleteMapping("/{reportId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal Long userId, @PathVariable Long reportId) {
        admissionReportService.delete(userId, reportId);
        return ResponseEntity.ok(ApiResponse.ok("합격·탈락 스펙이 삭제되었습니다.", null));
    }
}