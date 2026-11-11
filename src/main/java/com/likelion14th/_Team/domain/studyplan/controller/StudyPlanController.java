package com.likelion14th._Team.domain.studyplan.controller;

import com.likelion14th._Team.domain.studyplan.dto.StudyPlanCreateRequest;
import com.likelion14th._Team.domain.studyplan.dto.StudyPlanCreateResponse;
import com.likelion14th._Team.domain.studyplan.dto.StudyPlanDeleteResponse;
import com.likelion14th._Team.domain.studyplan.dto.StudyPlanItemCreateResponse;
import com.likelion14th._Team.domain.studyplan.dto.StudyPlanItemDeleteResponse;
import com.likelion14th._Team.domain.studyplan.dto.StudyPlanItemUpdateResponse;
import com.likelion14th._Team.domain.studyplan.service.StudyPlanItemService;
import com.likelion14th._Team.domain.studyplan.dto.StudyPlanDetail;
import com.likelion14th._Team.domain.studyplan.dto.StudyPlanRenameResponse;
import com.likelion14th._Team.domain.studyplan.dto.StudyPlanSummary;
import com.likelion14th._Team.domain.studyplan.service.StudyPlanService;
import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import com.likelion14th._Team.global.response.ApiResponse;
import com.likelion14th._Team.global.response.FieldErrorDetail;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import tools.jackson.databind.JsonNode;

@Slf4j
@RestController
@RequestMapping("/api/v1/study-plans")
@RequiredArgsConstructor
public class StudyPlanController {

    private final StudyPlanService studyPlanService;
    private final StudyPlanItemService studyPlanItemService;

    @Operation(summary = "수강계획 과목 삭제")
    @DeleteMapping("/{studyPlanId}/items/{itemId}")
    public ApiResponse<StudyPlanItemDeleteResponse> deleteItem(
            @AuthenticationPrincipal Long userId, @PathVariable String studyPlanId, @PathVariable String itemId) {
        long planId = parseStudyPlanId(studyPlanId);
        long courseId = parsePositiveId(itemId, "itemId");
        try {
            StudyPlanItemDeleteResponse result = studyPlanItemService.delete(userId, planId, courseId);
            String timestamp = OffsetDateTime.now(ZoneId.of("Asia/Seoul")).withNano(0)
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            return new ApiResponse<>("S200", "수강계획 과목을 삭제했습니다.", result, null, timestamp);
        } catch (CustomException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error("수강계획 과목 삭제 실패", exception);
            throw new CustomException(ErrorCode.INTERNAL_ERROR, "수강계획 과목 삭제 중 오류가 발생했습니다.");
        }
    }

    @Hidden
    @DeleteMapping("/{studyPlanId}/items")
    public void deleteWithoutItemId(@PathVariable String studyPlanId) {
        parseStudyPlanId(studyPlanId);
        throw invalidId("itemId");
    }

    @Operation(summary = "수강계획 과목 수정", description = "외국 학점과 본교 인정 과목만 수정합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(mediaType = "application/json", schema = @Schema(type = "object",
                            example = "{\"foreignCredit\":5.0,\"homeCourseId\":null}"))))
    @PatchMapping("/{studyPlanId}/items/{itemId}")
    public ApiResponse<StudyPlanItemUpdateResponse> updateItem(
            @AuthenticationPrincipal Long userId, @PathVariable String studyPlanId, @PathVariable String itemId,
            @RequestBody(required = false) JsonNode body) {
        long planId = parseStudyPlanId(studyPlanId);
        long courseId = parsePositiveId(itemId, "itemId");
        try {
            StudyPlanItemUpdateResponse result = studyPlanItemService.update(userId, planId, courseId, body);
            return new ApiResponse<>("S200", "수강계획 과목을 수정했습니다.", result, null,
                    result.updatedAt().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        } catch (CustomException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error("수강계획 과목 수정 실패", exception);
            throw new CustomException(ErrorCode.INTERNAL_ERROR, "수강계획 과목 수정 중 오류가 발생했습니다.");
        }
    }

    @Operation(summary = "수강계획 과목 추가",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(mediaType = "application/json", schema = @Schema(type = "object",
                            example = "{\"itemType\":\"CUSTOM\",\"courseName\":\"Business Strategy\","
                                    + "\"foreignCredit\":6.0,\"sourceCourseId\":null,\"homeCourseId\":null,"
                                    + "\"recognitionType\":\"GENERAL\"}"))))
    @PostMapping("/{studyPlanId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StudyPlanItemCreateResponse> createItem(
            @AuthenticationPrincipal Long userId, @PathVariable String studyPlanId,
            @RequestBody(required = false) JsonNode body) {
        long id = parseStudyPlanId(studyPlanId);
        try {
            StudyPlanItemCreateResponse result = studyPlanItemService.create(userId, id, body);
            String timestamp = OffsetDateTime.now(ZoneId.of("Asia/Seoul")).withNano(0)
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            return new ApiResponse<>("S201", "수강계획 과목을 추가했습니다.", result, null, timestamp);
        } catch (CustomException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error("수강계획 과목 추가 실패", exception);
            throw new CustomException(ErrorCode.INTERNAL_ERROR, "수강계획 과목 추가 중 오류가 발생했습니다.");
        }
    }

    @Operation(summary = "내 수강계획 목록 조회",
            description = "예상 인정 학점은 참고용이며 실제 학점 인정 여부는 학과에서 최종 확인해야 합니다.")
    @GetMapping
    public ApiResponse<List<StudyPlanSummary>> list(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok("수강계획 목록 조회에 성공했습니다.", studyPlanService.list(userId));
    }

    @Operation(summary = "수강계획 상세 조회")
    @GetMapping("/{studyPlanId}")
    public ApiResponse<StudyPlanDetail> detail(
            @AuthenticationPrincipal Long userId, @PathVariable Long studyPlanId) {
        if (studyPlanId <= 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "입력값이 올바르지 않습니다.");
        }
        try {
            return ApiResponse.ok("수강계획 상세 조회에 성공했습니다.", studyPlanService.detail(userId, studyPlanId));
        } catch (CustomException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error("수강계획 상세 조회 실패", exception);
            throw new CustomException(ErrorCode.INTERNAL_ERROR, "수강계획 상세 조회 중 오류가 발생했습니다.");
        }
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidPath() {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT.getCode(), "입력값이 올바르지 않습니다.", null));
    }

    @Operation(summary = "수강계획 이름 수정", requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true, content = @Content(mediaType = "application/json",
                    schema = @Schema(type = "object", example = "{\"name\":\"2027-1 최종 수강계획\"}"))))
    @PatchMapping("/{studyPlanId}")
    public ApiResponse<StudyPlanRenameResponse> rename(
            @AuthenticationPrincipal Long userId, @PathVariable String studyPlanId,
            @RequestBody(required = false) JsonNode body) {
        long id = parseStudyPlanId(studyPlanId);
        try {
            StudyPlanRenameResponse result = studyPlanService.rename(userId, id, body == null ? null : body.get("name"));
            return new ApiResponse<>("S200", "수강계획 이름을 수정했습니다.", result, null,
                    result.updatedAt().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        } catch (CustomException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error("수강계획 이름 수정 실패", exception);
            throw new CustomException(ErrorCode.INTERNAL_ERROR, "수강계획 이름 수정 중 오류가 발생했습니다.");
        }
    }

    @Operation(summary = "수강계획 삭제")
    @DeleteMapping("/{studyPlanId}")
    public ApiResponse<StudyPlanDeleteResponse> delete(
            @AuthenticationPrincipal Long userId, @PathVariable String studyPlanId) {
        long id = parseStudyPlanId(studyPlanId);
        try {
            StudyPlanDeleteResponse result = studyPlanService.delete(userId, id);
            String timestamp = OffsetDateTime.now(ZoneId.of("Asia/Seoul")).withNano(0)
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            return new ApiResponse<>("S200", "수강계획을 삭제했습니다.", result, null, timestamp);
        } catch (CustomException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error("수강계획 삭제 실패", exception);
            throw new CustomException(ErrorCode.INTERNAL_ERROR, "수강계획 삭제 중 오류가 발생했습니다.");
        }
    }

    @Hidden
    @RequestMapping(method = {RequestMethod.PATCH, RequestMethod.DELETE})
    public void missingStudyPlanId() {
        throw invalidStudyPlanId();
    }

    private long parseStudyPlanId(String studyPlanId) {
        return parsePositiveId(studyPlanId, "studyPlanId");
    }

    private long parsePositiveId(String value, String field) {
        if (value.matches("[0-9]+")) {
            try {
                long id = Long.parseLong(value);
                if (id > 0) {
                    return id;
                }
            } catch (NumberFormatException exception) {
                throw invalidId(field);
            }
        }
        throw invalidId(field);
    }

    private CustomException invalidStudyPlanId() {
        return invalidId("studyPlanId");
    }

    private CustomException invalidId(String field) {
        return new CustomException(ErrorCode.INVALID_INPUT, "입력값이 올바르지 않습니다.",
                List.of(new FieldErrorDetail(field, "양의 정수를 입력해 주세요.")));
    }

    @Operation(summary = "수강계획 생성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StudyPlanCreateResponse> create(
            @AuthenticationPrincipal Long userId, @Valid @RequestBody StudyPlanCreateRequest request) {
        return ApiResponse.created("수강계획을 생성했습니다.", studyPlanService.create(userId, request));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ApiResponse<Void>> handleInvalidRequest(HttpServletRequest request) {
        if (request.getRequestURI().contains("/items")) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ErrorCode.INVALID_INPUT.getCode(),
                    "입력값이 올바르지 않습니다.", List.of(new FieldErrorDetail("body", "올바른 JSON 본문을 입력해 주세요."))));
        }
        if ("PATCH".equals(request.getMethod())) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ErrorCode.INVALID_INPUT.getCode(),
                    "입력값이 올바르지 않습니다.", List.of(new FieldErrorDetail("name", "올바른 JSON 본문을 입력해 주세요."))));
        }
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT.getCode(), "요청 값이 올바르지 않습니다.", null));
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustom(CustomException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode.getCode(), exception.getMessage(), exception.getErrors()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleFailure(RuntimeException exception) {
        log.error("수강계획 요청 처리 실패", exception);
        return ResponseEntity.internalServerError()
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR.getCode(), "서버 내부 오류가 발생했습니다.", null));
    }
}
