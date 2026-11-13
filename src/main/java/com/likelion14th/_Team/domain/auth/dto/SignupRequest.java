package com.likelion14th._Team.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.likelion14th._Team.global.validation.NicknameRules;
import com.likelion14th._Team.global.enums.LanguageTestType;
import com.likelion14th._Team.global.enums.MajorType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record SignupRequest(

        @NotBlank(message = "학교 메일을 입력해 주세요")
        @Email(message = "이메일 형식이 올바르지 않습니다")
        @Size(max = 100, message = "학교 메일은 100자 이하여야 합니다")
        String schoolEmail,

        @NotBlank(message = "비밀번호를 입력해 주세요")
        @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하여야 합니다")
        String password,

        @NotBlank(message = "닉네임을 입력해 주세요")
        @Pattern(regexp = NicknameRules.REGEXP, message = NicknameRules.FORMAT_MESSAGE)
        String nickname,

        @NotNull(message = "전공 구분을 선택해 주세요")
        MajorType majorType,

        @NotNull(message = "약관 동의 정보가 필요합니다")
        @Valid
        ConsentsRequest consents,

        @NotNull(message = "누적 학점을 입력해 주세요")
        @DecimalMin(value = "0.0", message = "누적 학점은 0 이상이어야 합니다")
        @DecimalMax(value = "4.5", message = "누적 학점은 4.5 이하여야 합니다")
        @Digits(integer = 1, fraction = 2, message = "누적 학점은 소수점 둘째 자리까지 입력할 수 있습니다")
        BigDecimal gpa,

        @Valid
        List<@NotNull(message = "어학 성적 항목이 비어 있습니다") @Valid LanguageScoreRequest> languageScores
) {

    /** languageScores를 안 보내거나 null이면 빈 배열로 취급 */
    public SignupRequest {
        nickname = NicknameRules.normalize(nickname);
        languageScores = (languageScores == null) ? List.of() : languageScores;
    }

    /** 같은 어학 시험(testType) 중복 불가 */
    @JsonIgnore
    @AssertTrue(message = "같은 어학 시험을 중복해서 등록할 수 없습니다")
    public boolean isLanguageScoresUnique() {
        List<LanguageTestType> types = languageScores.stream()
                .filter(Objects::nonNull)
                .map(LanguageScoreRequest::testType)
                .filter(Objects::nonNull)
                .toList();
        return types.size() == types.stream().distinct().count();
    }

    public record ConsentsRequest(
            @NotNull(message = "서비스 이용약관 동의 여부가 필요합니다")
            @AssertTrue(message = "서비스 이용약관에 동의해야 합니다")
            Boolean termsOfService,

            @NotNull(message = "개인정보 수집·이용 동의 여부가 필요합니다")
            @AssertTrue(message = "개인정보 수집·이용에 동의해야 합니다")
            Boolean privacyPolicy
    ) {
    }

    public record LanguageScoreRequest(
            @NotNull(message = "어학 시험 종류를 선택해 주세요")
            LanguageTestType testType,

            @NotNull(message = "어학 점수를 입력해 주세요")
            BigDecimal score
    ) {

        @JsonIgnore
        @AssertTrue(message = "점수가 해당 시험의 범위·단위에 맞지 않습니다")
        public boolean isScoreValid() {
            if (testType == null || score == null) {
                return true; // @NotNull에서 처리
            }
            return switch (testType) {
                case IELTS -> inRange("0", "9") && multipleOf("0.5");
                case TOEFL_ITP -> inRange("310", "677") && isInteger();
                case TOEFL_IBT -> inRange("0", "120") && isInteger();
                case TOEFL_NEW -> inRange("1", "6") && multipleOf("0.5");
            };
        }

        private boolean inRange(String min, String max) {
            return score.compareTo(new BigDecimal(min)) >= 0
                    && score.compareTo(new BigDecimal(max)) <= 0;
        }

        private boolean multipleOf(String step) {
            return score.remainder(new BigDecimal(step)).compareTo(BigDecimal.ZERO) == 0;
        }

        private boolean isInteger() {
            return score.stripTrailingZeros().scale() <= 0;
        }
    }
}