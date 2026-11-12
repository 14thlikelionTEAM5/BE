package com.likelion14th._Team.domain.university.controller;

import com.likelion14th._Team.domain.university.dto.UniversityReturnReportsResponse;
import com.likelion14th._Team.domain.university.service.UniversityReturnReportService;
import com.likelion14th._Team.global.exception.*;
import com.likelion14th._Team.global.response.*;
import com.likelion14th._Team.global.logging.ExceptionLogSanitizer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@SecurityRequirement(name = "bearerAuth")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/universities/{universityId}/return-reports")
public class UniversityReturnReportController {
    private final UniversityReturnReportService service;

    @GetMapping
    @Operation(summary = "귀국보고서 조회", description = "학기·생성일시·ID 내림차순으로 전체 보고서를 반환합니다. 저장된 요약을 사용하며 작성자 정보는 제외합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공. 보고서가 없으면 빈 배열"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "학교 ID 형식 오류"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인이 필요합니다."),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 학교입니다"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "귀국보고서 조회 중 오류가 발생했습니다")
    })
    public ApiResponse<UniversityReturnReportsResponse> reports(@PathVariable String universityId) {
        long id = parseId(universityId);
        try {
            return ApiResponse.ok("귀국보고서 조회에 성공했습니다.", service.reports(id));
        } catch (CustomException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("귀국보고서 조회 실패", ExceptionLogSanitizer.withoutMessages(e));
            throw new CustomException(ErrorCode.INTERNAL_ERROR, "귀국보고서 조회 중 오류가 발생했습니다");
        }
    }

    private long parseId(String value) {
        try {
            if (!value.matches("[0-9]+")) throw new NumberFormatException();
            long id = Long.parseLong(value);
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT, ErrorCode.INVALID_INPUT.getMessage(),
                    List.of(new FieldErrorDetail("universityId", "양의 정수를 입력해 주세요.")));
        }
    }
}
