package com.likelion14th._Team.domain.university.dto;

import com.likelion14th._Team.domain.university.entity.University;

public record UniversitySearchResponse(
        Long universityId,
        String name,
        String country
) {

    public static UniversitySearchResponse from(University university) {
        return new UniversitySearchResponse(university.getId(), university.getName(), university.getCountry());
    }
}
