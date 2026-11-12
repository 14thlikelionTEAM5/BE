package com.likelion14th._Team.domain.user.controller;

import com.likelion14th._Team.domain.user.dto.NicknameCheckResponse;
import com.likelion14th._Team.domain.user.service.NicknameCheckRateLimiter;
import com.likelion14th._Team.domain.user.service.NicknameCheckService;
import com.likelion14th._Team.global.response.ApiResponse;
import com.likelion14th._Team.global.response.FieldErrorDetail;
import com.likelion14th._Team.global.logging.ExceptionLogSanitizer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/nickname")
public class NicknameCheckController {
    private final NicknameCheckService service;
    private final NicknameCheckRateLimiter limiter;

    @GetMapping(value = "/check", produces = "application/json")
    @SecurityRequirements
    @Operation(summary = "닉네임 중복 확인", description = "인증 선택. 잘못된 토큰은 비로그인 처리. IP당 1분 30회까지 허용합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "사용 가능·중복·본인 닉네임"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 오류"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "요청 횟수 초과"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<NicknameCheckResponse>> check(
                                  @Parameter(required = true, description = "확인할 닉네임. 한글·영문·숫자 2~12자", example = "채원")
                                  @RequestParam(required = false) String nickname,
                                  @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
                                  HttpServletRequest request) {
        // 클라이언트가 임의로 보내는 X-Forwarded-For는 신뢰하지 않는다.
        if (!limiter.allow(request.getRemoteAddr())) {
            return ResponseEntity.status(429).body(response("E429", "요청 횟수를 초과했습니다. 잠시 후 다시 시도해 주세요.", null, null));
        }
        try {
            NicknameCheckResponse result = service.check(nickname, userId);
            String message = result.isCurrent() ? "현재 사용 중인 본인의 닉네임입니다."
                    : result.available() ? "사용할 수 있는 닉네임입니다." : "이미 사용 중인 닉네임입니다.";
            return ResponseEntity.ok(response("S200", message, result, null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(response("E400", "입력값이 올바르지 않습니다", null,
                    List.of(new FieldErrorDetail("nickname", e.getMessage()))));
        } catch (Exception e) {
            log.error("닉네임 확인 실패", ExceptionLogSanitizer.withoutMessages(e));
            return ResponseEntity.internalServerError().body(response("E500", "닉네임 확인 중 오류가 발생했습니다", null, null));
        }
    }

    private <T> ApiResponse<T> response(String code, String message, T data, List<FieldErrorDetail> errors) {
        return new ApiResponse<>(code, message, data, errors,
                OffsetDateTime.now(ZoneId.of("Asia/Seoul")).withNano(0).toString());
    }
}
