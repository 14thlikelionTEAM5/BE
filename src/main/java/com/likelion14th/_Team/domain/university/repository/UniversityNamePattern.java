package com.likelion14th._Team.domain.university.repository;

/** 두 학교 검색 쿼리에서 ESCAPE '!'와 함께 쓰는 문자 그대로의 부분 검색 패턴. */
public final class UniversityNamePattern {
    private UniversityNamePattern() {
    }

    public static String contains(String keyword) {
        return "%" + keyword.replace("!", "!!").replace("%", "!%")
                .replace("_", "!_") + "%";
    }
}
