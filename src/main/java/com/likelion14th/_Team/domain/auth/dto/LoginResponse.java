package com.likelion14th._Team.domain.auth.dto;

import com.likelion14th._Team.domain.user.entity.User;
import com.likelion14th._Team.global.enums.MajorType;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserInfo user
) {

    public static LoginResponse of(String accessToken, long expiresIn, User user) {
        return new LoginResponse(accessToken, "Bearer", expiresIn, UserInfo.from(user));
    }

    public record UserInfo(
            Long userId,
            String schoolEmail,
            String nickname,
            MajorType majorType
    ) {

        public static UserInfo from(User user) {
            return new UserInfo(
                    user.getId(),
                    user.getSchoolEmail(),
                    user.getNickname(),
                    user.getMajorType()
            );
        }
    }
}