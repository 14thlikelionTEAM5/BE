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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    @Operation(summary = "협정교 목록 조회", description = "최신 모집 학기의 협정교 목록을 조회합니다. 지원 가능 여부는 참고용입니다. NAME은 학교명(대소문자·악센트 무시) → ID 순, COUNTRY는 국가명 → 학교명 → ID 순입니다.")
    @ApiFailure(message = "협정교 목록 조회 중 오류가 발생했습니다.", logMessage = "협정교 목록 조회 실패")
    @GetMapping
    public ApiResponse<UniversityListResponse> list(@AuthenticationPrincipal Long userId,
            @Parameter(description = "학교명 부분 검색. 대소문자·악센트 무시(ecole, école, ÉCOLE 모두 일치). 앞뒤 공백 제거 후 최대 200자(유니코드 코드 포인트 기준). 미입력·빈 문자열·공백만 입력하면 검색 필터 없음. 내부 공백과 %, _, 역슬래시는 문자 그대로 검색합니다.")
            @RequestParam(required = false) String keyword,
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
    @Operation(summary = "학교 검색", description = """
            로그인한 사용자가 학교명 일부로 후보 학교를 최대 10건 조회합니다.
            협정 공고 존재 여부나 모집 학기에 관계없이 university에 등록된 전체 학교를 대상으로 합니다.
            검색어는 앞뒤 공백을 제거한 뒤 1~50자(유니코드 코드 포인트 기준)여야 합니다.
            누락·빈 문자열·공백만 입력하면 400입니다. 내부 공백은 유지합니다.
            %, _, 역슬래시는 와일드카드가 아닌 입력한 문자 그대로 검색합니다.
            협정교 목록 검색과 동일하게 대소문자와 악센트를 모두 무시합니다.
            예를 들어 ecole, école, ÉCOLE은 같은 학교명에 일치합니다.
            학교명은 대소문자·악센트를 무시한 이름순, 비교 결과가 같으면 학교 ID 오름차순으로 정렬합니다.
            일치하는 학교가 없으면 HTTP 200과 빈 배열을 반환합니다.
            """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회되었습니다.",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "code": "S200",
                              "message": "조회되었습니다.",
                              "data": [
                                {"universityId": 31, "name": "Nanyang Technological University", "country": "싱가포르"},
                                {"universityId": 12, "name": "National University of Singapore", "country": "싱가포르"}
                              ],
                              "errors": null,
                              "timestamp": "2026-11-14T12:00:00"
                            }
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값이 올바르지 않습니다. errors의 keyword 필드에 누락 또는 50자 초과 사유를 반환합니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인이 필요합니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "학교 검색 중 오류가 발생했습니다.")
    })
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<UniversitySearchResponse>>> search(
            @Parameter(description = "학교명 일부 (앞뒤 공백 제외 1~50자)", example = "Na", required = true,
                    schema = @Schema(minLength = 1, maxLength = 50))
            @RequestParam(required = false) String keyword) {
        List<UniversitySearchResponse> response = universityService.search(keyword);
        return ResponseEntity.ok(ApiResponse.ok("조회되었습니다.", response));
    }
}
