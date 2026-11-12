package com.likelion14th._Team.domain.admission.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.likelion14th._Team.global.enums.AdmissionResult;
import com.likelion14th._Team.global.enums.LanguageTestType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

/** 합격·탈락 스펙 저장(POST)·수정(PUT) 공통 요청 */
public record AdmissionReportRequest(

        @Schema(example = "12")
        @NotNull(message = "학교를 선택해 주세요")
        Long universityId,

        @Schema(description = "YYYY-1 또는 YYYY-2", example = "2027-1")
        @NotBlank(message = "학기를 입력해 주세요")
        @Pattern(regexp = "^\\d{4}-[12]$", message = "학기는 YYYY-1 또는 YYYY-2 형식이어야 합니다")
        String semester,

        @Schema(description = "PASS(합격) / FAIL(탈락)", example = "PASS")
        @NotNull(message = "합격 또는 탈락을 선택해 주세요")
        AdmissionResult result,

        @Schema(description = "어학 성적이 없으면 languageScore와 함께 null", example = "TOEFL_IBT")
        LanguageTestType languageTestType,

        @Schema(example = "100.0")
        BigDecimal languageScore
) {

    public AdmissionReportRequest {
        // 응답에서 100 대신 100.0으로 나가도록 소수 첫째 자리까지 맞춘다
        if (languageScore != null && languageScore.scale() < 1) {
            languageScore = languageScore.setScale(1);
        }
    }

    /** 어학 시험 종류와 점수는 함께 보내거나 둘 다 null */
    @JsonIgnore
    @AssertTrue(message = "어학 시험 종류와 점수는 함께 입력하거나 둘 다 비워 주세요")
    public boolean isLanguagePaired() {
        return (languageTestType == null) == (languageScore == null);
    }

    /** 시험별 점수 범위·단위 (회원가입의 어학 성적 검증과 같은 규칙) */
    @JsonIgnore
    @AssertTrue(message = "점수가 해당 시험의 범위·단위에 맞지 않습니다")
    public boolean isLanguageScoreValid() {
        if (languageTestType == null || languageScore == null) {
            return true; // isLanguagePaired에서 처리
        }
        return switch (languageTestType) {
            case IELTS -> inRange("0", "9") && multipleOf("0.5");
            case TOEFL_ITP -> inRange("310", "677") && isInteger();
            case TOEFL_IBT -> inRange("0", "120") && isInteger();
            case TOEFL_NEW -> inRange("1", "6") && multipleOf("0.5");
        };
    }

    private boolean inRange(String min, String max) {
        return languageScore.compareTo(new BigDecimal(min)) >= 0
                && languageScore.compareTo(new BigDecimal(max)) <= 0;
    }

    private boolean multipleOf(String step) {
        return languageScore.remainder(new BigDecimal(step)).compareTo(BigDecimal.ZERO) == 0;
    }

    private boolean isInteger() {
        return languageScore.stripTrailingZeros().scale() <= 0;
    }
}