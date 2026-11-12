package com.likelion14th._Team.domain.user.dto;

import com.likelion14th._Team.domain.user.dto.MyInfoResponse.LanguageScoreItem;
import com.likelion14th._Team.domain.user.entity.User;
import com.likelion14th._Team.domain.user.entity.UserLanguageScore;
import com.likelion14th._Team.domain.user.entity.UserProfile;
import com.likelion14th._Team.global.enums.MajorType;
import java.math.BigDecimal;
import java.util.List;

public record MyInfoUpdateResponse(
        Long userId,
        String schoolEmail,
        String nickname,
        MajorType majorType,
        BigDecimal gpa,
        List<LanguageScoreItem> languageScores
) {

    public static MyInfoUpdateResponse of(User user, UserProfile profile, List<UserLanguageScore> scores) {
        return new MyInfoUpdateResponse(
                user.getId(),
                user.getSchoolEmail(),
                user.getNickname(),
                user.getMajorType(),
                profile.getGpa(),
                LanguageScoreItem.listOf(scores)
        );
    }
}