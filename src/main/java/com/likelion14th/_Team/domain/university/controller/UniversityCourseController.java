package com.likelion14th._Team.domain.university.controller;

import com.likelion14th._Team.domain.university.dto.UniversityCoursesResponse;
import com.likelion14th._Team.domain.university.service.UniversityCourseService;
import com.likelion14th._Team.global.exception.*;
import com.likelion14th._Team.global.response.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/universities/{universityId}")
@SecurityRequirement(name = "bearerAuth")
public class UniversityCourseController {
    private final UniversityCourseService service;
    @Operation(summary = "수강 과목 목록 조회", description = "귀국보고서별 과목을 중복 제거 없이 반환하며 과목명, 과목 ID 순으로 정렬합니다.")
    @GetMapping("/courses")
    public ApiResponse<UniversityCoursesResponse> courses(@AuthenticationPrincipal Long userId,
            @PathVariable String universityId) {
        long id = parseUniversityId(universityId);
        try {
            return ApiResponse.ok("수강 과목 목록 조회에 성공했습니다.", service.courses(userId, id));
        } catch (CustomException exception) { throw exception; }
        catch (RuntimeException exception) {
            log.error("수강 과목 목록 조회 실패", exception);
            throw new CustomException(ErrorCode.INTERNAL_ERROR, "수강 과목 목록 조회 중 오류가 발생했습니다");
        }
    }
    private long parseUniversityId(String value) {
        try {
            if (!value.matches("[0-9]+")) throw new NumberFormatException();
            long id = Long.parseLong(value);
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException exception) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "입력값이 올바르지 않습니다",
                    List.of(new FieldErrorDetail("universityId", "양의 정수를 입력해 주세요.")));
        }
    }
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustom(CustomException exception) {
        return ResponseEntity.status(exception.getErrorCode().getStatus())
                .body(ApiResponse.error(exception.getErrorCode().getCode(), exception.getMessage(), exception.getErrors()));
    }
}
