package com.likelion14th._Team.domain.user.controller;

import com.likelion14th._Team.domain.user.dto.MyInfoResponse;
import com.likelion14th._Team.domain.user.dto.MyInfoUpdateRequest;
import com.likelion14th._Team.domain.user.dto.MyInfoUpdateResponse;
import com.likelion14th._Team.domain.user.service.MyInfoService;
import com.likelion14th._Team.global.response.ApiResponse;
import com.likelion14th._Team.domain.user.dto.PasswordChangeRequest;
import com.likelion14th._Team.domain.user.service.PasswordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "MyInfo", description = "내 정보 API")
@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class MyInfoController {

    private final MyInfoService myInfoService;
    private final PasswordService passwordService;

    @Operation(summary = "내 정보 조회",
            description = "로그인한 사용자의 프로필, 지원 조건(누적 학점·어학 성적), 외부 AI 처리 동의 여부를 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<MyInfoResponse>> getMyInfo(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.ok("조회되었습니다.", myInfoService.getMyInfo(userId)));
    }

    @Operation(summary = "내 정보 수정",
            description = "닉네임, 전공 구분, 누적 학점은 보낸 항목만 수정합니다. "
                    + "languageScores는 생략하면 유지, 보내면 목록 전체로 교체(빈 배열이면 전체 삭제)합니다.")
    @PatchMapping
    public ResponseEntity<ApiResponse<MyInfoUpdateResponse>> updateMyInfo(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody MyInfoUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("수정되었습니다.", myInfoService.update(userId, request)));
    }
    @Operation(summary = "비밀번호 변경",
            description = "현재 비밀번호를 확인한 뒤 새 비밀번호로 변경합니다. 기존 토큰은 만료 시간까지 유효합니다.")
    @PatchMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PasswordChangeRequest request) {
        passwordService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.ok("비밀번호가 변경되었습니다.", null));
    }
}