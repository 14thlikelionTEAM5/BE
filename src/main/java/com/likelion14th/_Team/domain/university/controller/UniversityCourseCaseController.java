package com.likelion14th._Team.domain.university.controller;

import com.likelion14th._Team.domain.university.dto.UniversityCourseCasesResponse;
import com.likelion14th._Team.domain.university.service.UniversityCourseCaseService;
import com.likelion14th._Team.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import com.likelion14th._Team.global.exception.ApiFailure;
import com.likelion14th._Team.global.validation.PositiveIdParser;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/universities/{universityId}/course-cases")
public class UniversityCourseCaseController {
    private final UniversityCourseCaseService service;

    @ApiFailure(message = "인정 사례 목록 조회 중 오류가 발생했습니다", logMessage = "인정 사례 목록 조회 실패", omitExceptionMessage = true)
    @GetMapping
    @Operation(summary = "인정 사례 목록 조회", description = "전공 필터 없이 전체 인정 사례를 학기·등록일·ID 내림차순으로 반환합니다. 참고용이며 최종 인정 여부는 학과 확인이 필요합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인정 사례 목록. 없으면 빈 배열"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "학교 ID 형식 오류"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인이 필요합니다."),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 학교입니다"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "인정 사례 목록 조회 중 오류가 발생했습니다")
    })
    public ApiResponse<UniversityCourseCasesResponse> cases(@PathVariable String universityId) {
        long id = PositiveIdParser.parse(universityId, "universityId");
        return ApiResponse.ok("인정 사례 목록 조회에 성공했습니다.", service.cases(id));
    }
}
