package com.likelion14th._Team.domain.user.controller;

import com.likelion14th._Team.domain.user.dto.ConsentRequest;
import com.likelion14th._Team.domain.user.dto.ConsentResponse;
import com.likelion14th._Team.domain.user.dto.ConsentResult;
import com.likelion14th._Team.domain.user.service.ConsentService;
import com.likelion14th._Team.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Consent", description = "동의 기록 API")
@RestController
@RequestMapping("/api/v1/consents")
@RequiredArgsConstructor
public class ConsentController {

    private final ConsentService consentService;

    @Operation(summary = "동의 기록",
            description = "외부 AI 처리 동의(EXTERNAL_AI)를 기록합니다. 처음 기록하면 201, 이미 동의한 기록이 있으면 기존 기록을 200으로 반환합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<ConsentResponse>> record(
            @AuthenticationPrincipal Long userId, @RequestBody ConsentRequest request) {
        ConsentResult result = consentService.record(userId, request);
        if (result.created()) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.created("동의가 기록되었습니다.", result.consent()));
        }
        return ResponseEntity.ok(ApiResponse.ok("이미 동의한 기록입니다.", result.consent()));
    }
}
