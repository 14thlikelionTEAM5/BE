package com.likelion14th._Team.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.likelion14th._Team.global.validation.NicknameRules;
import com.likelion14th._Team.domain.auth.dto.SignupRequest.LanguageScoreRequest;
import com.likelion14th._Team.global.enums.LanguageTestType;
import com.likelion14th._Team.global.enums.MajorType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * 모든 필드는 선택. null(생략)이면 해당 항목은 수정하지 않는다.
 * languageScores는 null이면 유지, 빈 배열이면 전체 삭제, 목록이 있으면 그 목록으로 교체.
 * (SignupRequest처럼 null을 빈 배열로 바꾸면 생략만 해도 어학 성적이 지워지므로 그대로 둔다)
 */
public record MyInfoUpdateRequest(

        @Pattern(regexp = NicknameRules.REGEXP, message = NicknameRules.FORMAT_MESSAGE)
        String nickname,

        MajorType majorType,

        @DecimalMin(value = "0.0", message = "누적 학점은 0 이상이어야 합니다")
        @DecimalMax(value = "4.5", message = "누적 학점은 4.5 이하여야 합니다")
        @Digits(integer = 1, fraction = 2, message = "누적 학점은 소수점 둘째 자리까지 입력할 수 있습니다")
        BigDecimal gpa,

        @Valid
        List<@NotNull(message = "어학 성적 항목이 비어 있습니다") @Valid LanguageScoreRequest> languageScores
) {

    public MyInfoUpdateRequest {
        nickname = NicknameRules.normalize(nickname);
    }

    /** 같은 어학 시험(testType) 중복 불가 */
    @JsonIgnore
    @AssertTrue(message = "같은 어학 시험을 중복해서 등록할 수 없습니다")
    public boolean isLanguageScoresUnique() {
        if (languageScores == null) {
            return true;
        }
        List<LanguageTestType> types = languageScores.stream()
                .filter(Objects::nonNull)
                .map(LanguageScoreRequest::testType)
                .filter(Objects::nonNull)
                .toList();
        return types.size() == types.stream().distinct().count();
    }
}