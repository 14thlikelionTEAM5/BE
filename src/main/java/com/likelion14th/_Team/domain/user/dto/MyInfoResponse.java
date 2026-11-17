package com.likelion14th._Team.domain.user.dto;

import com.likelion14th._Team.domain.user.entity.User;
import com.likelion14th._Team.domain.user.entity.UserLanguageScore;
import com.likelion14th._Team.domain.user.entity.UserProfile;
import com.likelion14th._Team.global.enums.LanguageTestType;
import com.likelion14th._Team.global.enums.MajorType;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

public record MyInfoResponse(
        Long userId,
        String schoolEmail,
        String nickname,
        MajorType majorType,
        BigDecimal gpa,
        List<LanguageScoreItem> languageScores,
        boolean externalAiConsented
) {

    public static MyInfoResponse of(User user, UserProfile profile,
                                    List<UserLanguageScore> scores, boolean externalAiConsented) {
        return new MyInfoResponse(
                user.getId(),
                user.getSchoolEmail(),
                user.getNickname(),
                user.getMajorType(),
                profile.getGpa(),
                LanguageScoreItem.listOf(scores),
                externalAiConsented
        );
    }

    public record LanguageScoreItem(LanguageTestType testType, BigDecimal score) {

        /** 응답 순서는 LanguageTestType 선언 순서 */
        public static List<LanguageScoreItem> listOf(List<UserLanguageScore> scores) {
            return scores.stream()
                    .sorted(Comparator.comparing(UserLanguageScore::getTestType))
                    .map(s -> new LanguageScoreItem(s.getTestType(), s.getScore()))
                    .toList();
        }
    }
}