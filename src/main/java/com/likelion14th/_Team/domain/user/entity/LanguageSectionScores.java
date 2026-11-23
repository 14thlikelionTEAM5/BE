package com.likelion14th._Team.domain.user.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.likelion14th._Team.global.enums.LanguageTestType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/** 같은 성적표의 영역별 점수. 기존 총점만 있는 성적은 이 값 전체가 null이다. */
@Embeddable
@Schema(description = "IELTS: 0~9, 0.5 단위 / TOEFL_IBT(구 점수): 0~30 정수 / TOEFL_NEW: 1~6, 0.5 단위. 입력하면 네 영역 모두 필수. TOEFL_ITP는 지원하지 않음")
public record LanguageSectionScores(
        @NotNull(message = "읽기 점수를 입력해 주세요")
        @Column(name = "reading_score", precision = 3, scale = 1) BigDecimal reading,
        @NotNull(message = "듣기 점수를 입력해 주세요")
        @Column(name = "listening_score", precision = 3, scale = 1) BigDecimal listening,
        @NotNull(message = "말하기 점수를 입력해 주세요")
        @Column(name = "speaking_score", precision = 3, scale = 1) BigDecimal speaking,
        @NotNull(message = "쓰기 점수를 입력해 주세요")
        @Column(name = "writing_score", precision = 3, scale = 1) BigDecimal writing
) {
    public List<BigDecimal> values() {
        return Arrays.asList(reading, listening, speaking, writing);
    }

    @JsonIgnore
    public boolean isComplete() {
        return values().stream().allMatch(value -> value != null);
    }

    public boolean validFor(LanguageTestType testType) {
        if (testType == LanguageTestType.TOEFL_ITP) return false;
        // 누락은 각 영역의 @NotNull 검증이 처리한다.
        return values().stream().filter(value -> value != null).allMatch(value -> switch (testType) {
            case IELTS -> valid(value, "0", "9", "0.5");
            case TOEFL_IBT -> valid(value, "0", "30", "1");
            case TOEFL_NEW -> valid(value, "1", "6", "0.5");
            case TOEFL_ITP -> false;
        });
    }

    private boolean valid(BigDecimal value, String min, String max, String step) {
        return value.compareTo(new BigDecimal(min)) >= 0 && value.compareTo(new BigDecimal(max)) <= 0
                && value.remainder(new BigDecimal(step)).signum() == 0;
    }
}
