package com.likelion14th._Team.domain.auth.controller;

import com.likelion14th._Team.domain.auth.dto.EmailSendRequest;
import com.likelion14th._Team.domain.auth.dto.EmailSendResponse;
import com.likelion14th._Team.domain.auth.dto.EmailVerifyRequest;
import com.likelion14th._Team.domain.auth.dto.EmailVerifyResponse;
import com.likelion14th._Team.domain.auth.dto.LoginRequest;
import com.likelion14th._Team.domain.auth.dto.LoginResponse;
import com.likelion14th._Team.domain.auth.dto.SignupRequest;
import com.likelion14th._Team.domain.auth.dto.SignupResponse;
import com.likelion14th._Team.domain.auth.service.AuthService;
import com.likelion14th._Team.domain.auth.service.EmailAuthService;
import com.likelion14th._Team.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailAuthService emailAuthService;

    @Operation(summary = "회원가입", description = "닉네임을 NFC로 정규화한 후 검증·중복 확인·저장합니다. 응답 nickname은 저장된 NFC 표기입니다.")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("회원가입이 완료되었습니다.", response));
    }

    @Operation(summary = "로그인")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("로그인되었습니다.", response));
    }

    @Operation(summary = "학교 메일 인증 발송")
    @PostMapping("/email/send")
    public ResponseEntity<ApiResponse<EmailSendResponse>> sendEmailCode(@Valid @RequestBody EmailSendRequest request) {
        EmailSendResponse response = emailAuthService.sendCode(request);
        return ResponseEntity.ok(ApiResponse.ok("인증 메일이 발송되었습니다.", response));
    }

    @Operation(summary = "학교 메일 인증 확인")
    @PostMapping("/email/verify")
    public ResponseEntity<ApiResponse<EmailVerifyResponse>> verifyEmailCode(@Valid @RequestBody EmailVerifyRequest request) {
        EmailVerifyResponse response = emailAuthService.verifyCode(request);
        return ResponseEntity.ok(ApiResponse.ok("학교 메일 인증이 완료되었습니다.", response));
    }
}