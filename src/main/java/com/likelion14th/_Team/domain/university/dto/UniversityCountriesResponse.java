package com.likelion14th._Team.domain.university.dto;

import java.util.List;

public record UniversityCountriesResponse(List<Country> countries) {
    public record Country(String country, Long universityCount) {}
}
