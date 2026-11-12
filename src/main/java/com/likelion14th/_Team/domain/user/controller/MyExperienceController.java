package com.likelion14th._Team.domain.user.controller;

import com.likelion14th._Team.domain.user.dto.MyExperiencesResponse;
import com.likelion14th._Team.domain.user.service.MyExperienceService;
import com.likelion14th._Team.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "MyExperience", description = "내 등록 내역 API")
@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class MyExperienceController {

    private final MyExperienceService myExperienceService;

    @Operation(summary = "내 등록 내역 조회",
            description = "내가 등록한 합격·탈락 스펙, 귀국보고서(수강 교과목 포함), 인정 사례를 한 번에 조회합니다. "
                    + "내역이 없는 항목은 빈 배열입니다.")
    @GetMapping("/experiences")
    public ResponseEntity<ApiResponse<MyExperiencesResponse>> getMyExperiences(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.ok("조회되었습니다.", myExperienceService.getMyExperiences(userId)));
    }
}