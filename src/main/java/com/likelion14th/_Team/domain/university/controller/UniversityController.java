package com.likelion14th._Team.domain.university.controller;

import com.likelion14th._Team.domain.university.dto.*;
import com.likelion14th._Team.domain.university.service.UniversityQueryService;
import com.likelion14th._Team.domain.university.service.UniversityService;
import com.likelion14th._Team.global.exception.*;
import com.likelion14th._Team.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import com.likelion14th._Team.global.validation.PositiveIdParser;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import java.util.List;
import com.likelion14th._Team.global.response.FieldErrorDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/universities")
@SecurityRequirement(name = "bearerAuth")
public class UniversityController {
    private final UniversityQueryService service;
    private final UniversityService universityService;

    @Operation(summary = "협정교 상세 조회", description = "학기 생략 시 해당 학교의 최신 공고를 조회합니다. 예상 인정 학점은 계산하지 않습니다.")
    @ApiFailure(message = "협정교 상세 조회 중 오류가 발생했습니다", logMessage = "협정교 상세 조회 실패")
    @GetMapping("/{universityId}")
    public ApiResponse<UniversityDetailResponse> detail(@AuthenticationPrincipal Long userId,
            @PathVariable String universityId,
            @Parameter(description = "YYYY-1 또는 YYYY-2", example = "2027-1")
            @RequestParam(required = false) String semester) {
        long id = PositiveIdParser.parse(universityId, "universityId");
        if (semester != null && !semester.matches("[0-9]{4}-[12]")) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "학기 형식이 올바르지 않습니다 (예: 2027-1)",
                    List.of(new FieldErrorDetail("semester", "YYYY-1 또는 YYYY-2 형식으로 입력해 주세요.")));
        }
        return ApiResponse.ok("협정교 상세 조회에 성공했습니다.", service.detail(userId, id, semester));
    }

    @Operation(summary = "국가 목록 조회", description = "모집 학기와 관계없이 전체 협정교를 국가별로 집계하여 가나다순으로 반환합니다.")
    @ApiFailure(message = "국가 목록 조회 중 오류가 발생했습니다", logMessage = "국가 목록 조회 실패")
    @GetMapping("/countries")
    public ApiResponse<UniversityCountriesResponse> countries(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok("국가 목록 조회에 성공했습니다.", service.countries(userId));
    }

    @Operation(summary = "협정교 목록 조회", description = "최신 모집 학기의 협정교 목록을 조회합니다. 지원 가능 여부는 참고용입니다.")
    @ApiFailure(message = "협정교 목록 조회 중 오류가 발생했습니다.", logMessage = "협정교 목록 조회 실패")
    @GetMapping
    public ApiResponse<UniversityListResponse> list(@AuthenticationPrincipal Long userId,
            @Parameter(description = "학교명 부분 검색, 대소문자 무시") @RequestParam(required = false) String keyword,
            @Parameter(description = "국가명 하나, 미입력 시 전체") @RequestParam(required = false) String country,
            @Parameter(schema = @Schema(allowableValues = {"TOEFL_IBT", "TOEFL_NEW", "TOEFL_ITP", "IELTS"}))
            @RequestParam(required = false) String languageTest,
            @Parameter(schema = @Schema(allowableValues = {"true", "false"}, defaultValue = "false"))
            @RequestParam(required = false) String eligibleOnly,
            @Parameter(schema = @Schema(allowableValues = {"NAME", "COUNTRY"}, defaultValue = "NAME"))
            @RequestParam(required = false) String sort) {
        UniversitySearch search = UniversitySearch.from(keyword, country, languageTest, eligibleOnly, sort);
        return ApiResponse.ok("협정교 목록 조회에 성공했습니다.", service.list(userId, search));

    }
    @Operation(summary = "학교 검색", description = "학교명 일부로 후보 학교를 이름순 최대 10건 조회합니다. 일치하는 학교가 없으면 빈 배열을 반환합니다.")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<UniversitySearchResponse>>> search(
            @Parameter(description = "학교명 일부 (앞뒤 공백 제외 1~50자)", required = true)
            @RequestParam(required = false) String keyword) {
        List<UniversitySearchResponse> response = universityService.search(keyword);
        return ResponseEntity.ok(ApiResponse.ok("조회되었습니다.", response));
    }
}
