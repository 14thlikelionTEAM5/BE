package com.likelion14th._Team.domain.auth.dto;

import com.likelion14th._Team.domain.user.entity.User;
import com.likelion14th._Team.global.enums.MajorType;

public record SignupResponse(
        Long userId,
        String schoolEmail,
        String nickname,
        MajorType majorType
) {

    public static SignupResponse from(User user) {
        return new SignupResponse(
                user.getId(),
                user.getSchoolEmail(),
                user.getNickname(),
                user.getMajorType()
        );
    }
}