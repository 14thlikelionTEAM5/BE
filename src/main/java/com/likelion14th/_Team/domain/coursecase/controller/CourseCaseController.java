package com.likelion14th._Team.domain.coursecase.controller;

import com.likelion14th._Team.domain.coursecase.dto.CourseCaseRequest;
import com.likelion14th._Team.domain.coursecase.dto.CourseCaseResponse;
import com.likelion14th._Team.domain.coursecase.service.CourseCaseService;
import com.likelion14th._Team.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "CourseCase", description = "인정 사례 API")
@RestController
@RequestMapping("/api/v1/course-cases")
@RequiredArgsConstructor
public class CourseCaseController {

    private final CourseCaseService courseCaseService;

    @Operation(summary = "인정 사례 저장",
            description = "내 귀국보고서 수강 교과목 한 건의 본교 학점 인정 결과를 등록합니다. 과목 하나당 1건만 등록할 수 있습니다. "
                    + "교양이면 homeCourseId는 null로 보냅니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<CourseCaseResponse>> save(
            @AuthenticationPrincipal Long userId, @Valid @RequestBody CourseCaseRequest request) {
        CourseCaseResponse response = courseCaseService.save(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("인정 사례가 등록되었습니다.", response));
    }

    @Operation(summary = "인정 사례 수정",
            description = "등록한 인정 사례를 덮어씁니다. 전공 구분은 등록 시점 값을 유지하고, "
                    + "대상 과목을 바꾸면 교환 학기도 새 과목의 학기로 바뀝니다.")
    @PutMapping("/{caseId}")
    public ResponseEntity<ApiResponse<CourseCaseResponse>> update(
            @AuthenticationPrincipal Long userId, @PathVariable Long caseId,
            @Valid @RequestBody CourseCaseRequest request) {
        CourseCaseResponse response = courseCaseService.update(userId, caseId, request);
        return ResponseEntity.ok(ApiResponse.ok("인정 사례가 수정되었습니다.", response));
    }

    @Operation(summary = "인정 사례 삭제",
            description = "인정 사례만 삭제합니다. 귀국보고서와 수강 교과목은 그대로 유지됩니다.")
    @DeleteMapping("/{caseId}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal Long userId, @PathVariable Long caseId) {
        courseCaseService.delete(userId, caseId);
        return ResponseEntity.ok(ApiResponse.ok("인정 사례가 삭제되었습니다.", null));
    }
}