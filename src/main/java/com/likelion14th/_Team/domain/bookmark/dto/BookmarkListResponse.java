package com.likelion14th._Team.domain.bookmark.dto;

import com.likelion14th._Team.domain.university.dto.UniversityListResponse.Eligibility;
import com.likelion14th._Team.domain.university.dto.UniversityListResponse.LanguageRequirement;
import java.math.BigDecimal;
import java.util.List;

public record BookmarkListResponse(String semester, int totalCount, List<School> universities) {
    public record School(Long universityId, String name, String country, Long agreementId,
                         Integer quota, BigDecimal minGpa, List<LanguageRequirement> languageRequirements,
                         Eligibility eligibility, boolean isBookmarked) {}
}
