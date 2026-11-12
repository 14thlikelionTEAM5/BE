package com.likelion14th._Team.domain.bookmark.controller;

import com.likelion14th._Team.domain.bookmark.dto.BookmarkCreateRequest;
import com.likelion14th._Team.domain.bookmark.dto.BookmarkCreateResponse;
import com.likelion14th._Team.domain.bookmark.dto.BookmarkDeleteResponse;
import com.likelion14th._Team.domain.bookmark.dto.BookmarkListResponse;
import com.likelion14th._Team.domain.bookmark.service.BookmarkService;
import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import com.likelion14th._Team.global.response.ApiResponse;
import com.likelion14th._Team.global.logging.ExceptionLogSanitizer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;
import tools.jackson.databind.JsonNode;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/bookmarks")
@SecurityRequirement(name = "bearerAuth")
public class BookmarkController {
    private final BookmarkService service;

    @Operation(summary = "찜 추가", description = "로그인한 사용자의 찜 목록에 학교를 추가합니다. 이미 찜한 학교는 409를 반환합니다.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            content = @Content(schema = @Schema(implementation = BookmarkCreateRequest.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ApiResponse<BookmarkCreateResponse>> create(
            @AuthenticationPrincipal Long userId, @RequestBody(required = false) JsonNode body) {
        BookmarkCreateRequest request = BookmarkCreateRequest.from(body);
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.created("찜 목록에 추가했습니다.", service.create(userId, request.universityId())));
        } catch (CustomException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error("찜 추가 실패", ExceptionLogSanitizer.withoutMessages(exception));
            throw new CustomException(ErrorCode.INTERNAL_ERROR, "찜 추가 중 오류가 발생했습니다");
        }
    }


    @Operation(summary = "찜 해제", description = "로그인한 사용자의 찜을 삭제합니다. 찜하지 않은 학교는 404를 반환합니다.")
    @DeleteMapping("/{universityId}")
    public ApiResponse<BookmarkDeleteResponse> delete(
            @AuthenticationPrincipal Long userId, @PathVariable String universityId) {
        long id;
        try {
            if (!universityId.matches("[0-9]+")) throw new NumberFormatException();
            id = Long.parseLong(universityId);
            if (id <= 0) throw new NumberFormatException();
        } catch (NumberFormatException exception) {
            throw BookmarkCreateRequest.invalid("universityId", "양의 정수 학교 ID를 입력해 주세요.");
        }
        try {
            return ApiResponse.ok("찜을 해제했습니다.", service.delete(userId, id));
        } catch (CustomException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error("찜 해제 실패", ExceptionLogSanitizer.withoutMessages(exception));
            throw new CustomException(ErrorCode.INTERNAL_ERROR, "찜 해제 중 오류가 발생했습니다");
        }
    }


    @Operation(summary = "찜 목록 조회", description = "본인이 찜한 학교를 최근 찜한 순으로 전체 반환합니다. 지원 자격은 협정교 조회와 동일하게 판정합니다.")
    @GetMapping
    public ApiResponse<BookmarkListResponse> list(@AuthenticationPrincipal Long userId) {
        try {
            return ApiResponse.ok("찜 목록 조회에 성공했습니다.", service.list(userId));
        } catch (CustomException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error("찜 목록 조회 실패", ExceptionLogSanitizer.withoutMessages(exception));
            throw new CustomException(ErrorCode.INTERNAL_ERROR, "찜 목록 조회 중 오류가 발생했습니다");
        }
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableBody(HttpMessageNotReadableException exception) {
        CustomException error = BookmarkCreateRequest.invalid("body", "올바른 JSON 객체를 입력해 주세요.");
        return ResponseEntity.badRequest().body(ApiResponse.error(
                error.getErrorCode().getCode(), error.getMessage(), error.getErrors()));
    }
}
