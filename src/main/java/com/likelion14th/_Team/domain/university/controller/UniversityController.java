package com.likelion14th._Team.domain.university.controller;

import com.likelion14th._Team.domain.university.dto.UniversitySearchResponse;
import com.likelion14th._Team.domain.university.service.UniversityService;
import com.likelion14th._Team.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "University", description = "학교 API")
@RestController
@RequestMapping("/api/v1/universities")
@RequiredArgsConstructor
public class UniversityController {

    private final UniversityService universityService;

    @Operation(summary = "학교 검색", description = "학교명 일부로 후보 학교를 이름순 최대 10건 조회합니다. 일치하는 학교가 없으면 빈 배열을 반환합니다.")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<UniversitySearchResponse>>> search(
            @Parameter(description = "학교명 일부 (앞뒤 공백 제외 1~50자)", required = true)
            @RequestParam(required = false) String keyword) {
        List<UniversitySearchResponse> response = universityService.search(keyword);
        return ResponseEntity.ok(ApiResponse.ok("조회되었습니다.", response));
    }
}
