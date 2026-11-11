package com.likelion14th._Team.domain.university.service;

import com.likelion14th._Team.domain.university.dto.UniversitySearchResponse;
import com.likelion14th._Team.domain.university.repository.UniversityRepository;
import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import com.likelion14th._Team.global.util.SearchKeyword;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UniversityService {

    static final int SEARCH_LIMIT = 10;

    private final UniversityRepository universityRepository;

    /** 학교명 자동완성. 이름순 최대 10건, 없으면 빈 목록 */
    public List<UniversitySearchResponse> search(String rawKeyword) {
        String keyword = SearchKeyword.require(rawKeyword);
        try {
            return universityRepository
                    .findByNameContainingOrderByNameAsc(keyword, PageRequest.of(0, SEARCH_LIMIT))
                    .stream()
                    .map(UniversitySearchResponse::from)
                    .toList();
        } catch (RuntimeException e) {
            log.error("학교 검색 실패", e);
            throw new CustomException(ErrorCode.INTERNAL_ERROR, "학교 검색 중 오류가 발생했습니다.");
        }
    }
}
