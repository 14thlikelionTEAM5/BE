package com.likelion14th._Team.domain.admission.controller;

import com.likelion14th._Team.domain.admission.dto.*;
import com.likelion14th._Team.domain.admission.service.AdmissionReportStatsService;
import com.likelion14th._Team.global.exception.*;
import com.likelion14th._Team.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import com.likelion14th._Team.global.enums.AdmissionResult;
import com.likelion14th._Team.global.response.FieldErrorDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/universities/{universityId}/admission-reports")
@SecurityRequirement(name="bearerAuth")
public class AdmissionReportController {
    private final AdmissionReportStatsService service;

    @Operation(summary="합격·탈락 스펙 목록 조회",description="학기 최신순, 동일 학기는 최근 등록순으로 전체 사례를 반환합니다. 작성자 정보는 반환하지 않습니다.")
    @GetMapping
    public ApiResponse<AdmissionReportListResponse> list(@AuthenticationPrincipal Long userId,
            @PathVariable String universityId,
            @Parameter(schema=@Schema(allowableValues={"ALL","PASS","FAIL"}, defaultValue="ALL"))
            @RequestParam(required=false) String result) {
        var filter = AdmissionReportFilter.from(universityId, null, null);
        AdmissionResult selectedResult = null;
        if (result != null && !result.equals("ALL")) {
            try { selectedResult = AdmissionResult.valueOf(result); }
            catch (IllegalArgumentException exception) {
                throw new CustomException(ErrorCode.INVALID_INPUT, "입력값이 올바르지 않습니다",
                        List.of(new FieldErrorDetail("result", "ALL, PASS, FAIL 중 하나를 입력해 주세요.")));
            }
        }
        try {
            return ApiResponse.ok("조회되었습니다.", service.list(userId, filter.universityId(), selectedResult));
        } catch (CustomException exception) { throw exception; }
        catch (RuntimeException exception) {
            log.error("합격·탈락 스펙 목록 조회 실패", exception);
            throw new CustomException(ErrorCode.INTERNAL_ERROR, "합격·탈락 스펙 목록 조회 중 오류가 발생했습니다");
        }
    }
    @Operation(summary="합격·탈락 스펙 분포 조회",description="합격 사례의 GPA·시험별 최저~최고와 합격·탈락 건수, 로그인 사용자 점수를 반환합니다.")
    @GetMapping("/stats")
    public ApiResponse<AdmissionStatsResponse> stats(@AuthenticationPrincipal Long userId,
            @PathVariable String universityId,
            @Parameter(description="미입력 시 전체 학기",example="2026-1") @RequestParam(required=false) String semester,
            @Parameter(schema=@Schema(allowableValues={"PRIMARY","DOUBLE","MINOR"})) @RequestParam(required=false) String majorType) {
        var filter=AdmissionReportFilter.from(universityId,semester,majorType);
        try {
            return ApiResponse.ok("합격 스펙 분포 조회에 성공했습니다.",service.stats(userId,filter));
        } catch (CustomException exception) { throw exception; }
        catch (RuntimeException exception) {
            log.error("합격 스펙 분포 조회 실패",exception);
            throw new CustomException(ErrorCode.INTERNAL_ERROR,"합격 스펙 분포 조회 중 오류가 발생했습니다");
        }
    }
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustom(CustomException exception) {
        return ResponseEntity.status(exception.getErrorCode().getStatus())
                .body(ApiResponse.error(exception.getErrorCode().getCode(),exception.getMessage(),exception.getErrors()));
    }
}
