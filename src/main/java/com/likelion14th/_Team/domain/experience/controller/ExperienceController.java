package com.likelion14th._Team.domain.experience.controller;

import com.likelion14th._Team.domain.experience.dto.DuplicateCheckResponse;
import com.likelion14th._Team.domain.experience.service.ExperienceDuplicateService;
import com.likelion14th._Team.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Experience", description = "경험 등록 공통 API")
@RestController
@RequestMapping("/api/v1/experiences")
@RequiredArgsConstructor
public class ExperienceController {

    private final ExperienceDuplicateService experienceDuplicateService;

    @Operation(summary = "등록 중복 확인",
            description = "경험 등록 시작 단계에서 선택한 유형·학교·학기로 등록할 수 있는지 확인합니다. "
                    + "등록 가능하면 available=true, 불가능하면 available=false와 reason을 반환합니다.")
    @GetMapping("/check-duplicate")
    public ResponseEntity<ApiResponse<DuplicateCheckResponse>> checkDuplicate(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "등록 유형", required = true,
                    schema = @Schema(type = "string",
                            allowableValues = {"RETURN_REPORT", "PASS", "FAIL", "COURSE_CASE"}))
            @RequestParam(required = false) String type,
            @Parameter(description = "선택한 학교 ID", required = true)
            @RequestParam(required = false) Long universityId,
            @Parameter(description = "교환(지원) 학기 (YYYY-1 또는 YYYY-2)", required = true,
                    schema = @Schema(example = "2027-1"))
            @RequestParam(required = false) String semester) {
        DuplicateCheckResponse response =
                experienceDuplicateService.check(userId, type, universityId, semester);
        return ResponseEntity.ok(ApiResponse.ok("조회되었습니다.", response));
    }
}