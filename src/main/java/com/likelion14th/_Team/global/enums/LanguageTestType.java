package com.likelion14th._Team.global.enums;

/**
 * 어학시험 종류 (구토플 / 뉴토플 / 기관토플 / 아이엘츠).
 * 합격 스펙 languageScores 등 API 응답의 정렬 기준으로 선언 순서를 사용한다.
 * 순서를 변경하면 응답 순서도 달라지므로 API 명세 및 정렬 테스트를 함께 확인한다.
 */
public enum LanguageTestType {
    TOEFL_IBT,
    TOEFL_NEW,
    TOEFL_ITP,
    IELTS
}
