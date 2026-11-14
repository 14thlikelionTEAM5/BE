package com.likelion14th._Team.domain.university.controller;

import com.likelion14th._Team.domain.university.dto.UniversityCoursesResponse;
import com.likelion14th._Team.domain.university.service.UniversityCourseService;
import com.likelion14th._Team.global.exception.ApiFailure;
import com.likelion14th._Team.global.response.*;
import com.likelion14th._Team.global.validation.PositiveIdParser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/universities/{universityId}")
@SecurityRequirement(name = "bearerAuth")
public class UniversityCourseController {
    private final UniversityCourseService service;
    @Operation(summary = "수강 과목 목록 조회", description = "귀국보고서별 과목을 중복 제거 없이 반환하며 과목명, 과목 ID 순으로 정렬합니다.")
    @ApiFailure(message = "수강 과목 목록 조회 중 오류가 발생했습니다", logMessage = "수강 과목 목록 조회 실패")
    @GetMapping("/courses")
    public ApiResponse<UniversityCoursesResponse> courses(@AuthenticationPrincipal Long userId,
            @PathVariable String universityId) {
        long id = PositiveIdParser.parse(universityId, "universityId");
        return ApiResponse.ok("수강 과목 목록 조회에 성공했습니다.", service.courses(userId, id));
    }
}
