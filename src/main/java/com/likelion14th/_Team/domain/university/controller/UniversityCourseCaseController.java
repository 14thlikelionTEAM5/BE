package com.likelion14th._Team.domain.university.controller;

import com.likelion14th._Team.domain.university.dto.UniversityCourseCasesResponse;
import com.likelion14th._Team.domain.university.service.UniversityCourseCaseService;
import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import com.likelion14th._Team.global.response.ApiResponse;
import com.likelion14th._Team.global.response.FieldErrorDetail;
import com.likelion14th._Team.global.logging.ExceptionLogSanitizer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/universities/{universityId}/course-cases")
public class UniversityCourseCaseController {
    private final UniversityCourseCaseService service;

    @GetMapping(produces = "application/json")
    @Operation(summary = "인정 사례 목록 조회", description = "전공 필터 없이 전체 인정 사례를 학기·등록일·ID 내림차순으로 반환합니다. 참고용이며 최종 인정 여부는 학과 확인이 필요합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인정 사례 목록. 없으면 빈 배열"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "학교 ID 형식 오류"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인이 필요합니다."),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 학교입니다"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "인정 사례 목록 조회 중 오류가 발생했습니다")
    })
    public ApiResponse<UniversityCourseCasesResponse> cases(@PathVariable String universityId) {
        long id = parseId(universityId);
        try {
            return ApiResponse.ok("인정 사례 목록 조회에 성공했습니다.", service.cases(id));
        } catch (CustomException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("인정 사례 목록 조회 실패", ExceptionLogSanitizer.withoutMessages(e));
            throw new CustomException(ErrorCode.INTERNAL_ERROR, "인정 사례 목록 조회 중 오류가 발생했습니다");
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
