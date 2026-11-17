package com.likelion14th._Team.domain.course.controller;

import com.likelion14th._Team.domain.course.dto.HomeCourseSearchResponse;
import com.likelion14th._Team.domain.course.service.HomeCourseService;
import com.likelion14th._Team.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "HomeCourse", description = "본교 과목 API")
@RestController
@RequestMapping("/api/v1/home-courses")
@RequiredArgsConstructor
public class HomeCourseController {

    private final HomeCourseService homeCourseService;

    @Operation(summary = "본교 과목 검색",
            description = "선택한 인정 구분(전필·전선)의 경영대학 개설 과목을 과목명 일부로 이름순 최대 10건 조회합니다. "
                    + "일치하는 과목이 없으면 빈 배열을 반환합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<HomeCourseSearchResponse>>> search(
            @Parameter(description = "과목명 일부 (앞뒤 공백 제외 1~50자)", required = true)
            @RequestParam(required = false) String keyword,
            @Parameter(description = "인정 구분", required = true,
                    schema = @Schema(type = "string", allowableValues = {"MAJOR_REQUIRED", "MAJOR_ELECTIVE"}))
            @RequestParam(required = false) String recognitionType) {
        List<HomeCourseSearchResponse> response = homeCourseService.search(keyword, recognitionType);
        return ResponseEntity.ok(ApiResponse.ok("조회되었습니다.", response));
    }
}
