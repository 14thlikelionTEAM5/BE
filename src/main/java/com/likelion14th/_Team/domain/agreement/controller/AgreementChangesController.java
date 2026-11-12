package com.likelion14th._Team.domain.agreement.controller;

import com.likelion14th._Team.domain.agreement.dto.AgreementChangesResponse;
import com.likelion14th._Team.domain.agreement.service.AgreementChangesService;
import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.logging.ExceptionLogSanitizer;
import com.likelion14th._Team.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/agreements")
public class AgreementChangesController {
    private final AgreementChangesService service;

    @GetMapping("/changes")
    @Operation(summary = "이번 학기 변경 안내 조회", description = "최신 학기와 직전 데이터 학기를 비교합니다. 이전 학기가 없으면 변경 건수는 null입니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인이 필요합니다."),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "협정교 정보가 없습니다"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "변경 안내 조회 중 오류가 발생했습니다")
    })
    public ApiResponse<AgreementChangesResponse> changes() {
        return ApiResponse.ok("이번 학기 변경 안내 조회에 성공했습니다.", service.changes());
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> custom(CustomException e) {
        return ResponseEntity.status(e.getErrorCode().getStatus())
                .body(ApiResponse.error(e.getErrorCode().getCode(), e.getMessage(), e.getErrors()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> unexpected(Exception e) {
        log.error("변경 안내 조회 실패", ExceptionLogSanitizer.withoutMessages(e));
        return ResponseEntity.internalServerError().body(ApiResponse.error("E500", "변경 안내 조회 중 오류가 발생했습니다", null));
    }
}
