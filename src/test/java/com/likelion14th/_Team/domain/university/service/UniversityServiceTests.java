package com.likelion14th._Team.domain.university.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.likelion14th._Team.domain.university.dto.UniversitySearchResponse;
import com.likelion14th._Team.domain.university.entity.University;
import com.likelion14th._Team.domain.university.repository.UniversityRepository;
import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

class UniversityServiceTests {

    private final UniversityRepository repository = mock(UniversityRepository.class);
    private final UniversityService service = new UniversityService(repository);

    @Test
    void searchesWithTrimmedKeywordAndLimitTen() {
        University nus = University.builder().name("National University of Singapore").country("싱가포르").build();
        when(repository.findByNameContainingOrderByNameAsc(eq("Singapore"), any(Pageable.class)))
                .thenReturn(List.of(nus));

        List<UniversitySearchResponse> result = service.search("  Singapore ");

        assertEquals(1, result.size());
        assertEquals("National University of Singapore", result.getFirst().name());
        assertEquals("싱가포르", result.getFirst().country());
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findByNameContainingOrderByNameAsc(eq("Singapore"), pageable.capture());
        assertEquals(0, pageable.getValue().getPageNumber());
        assertEquals(10, pageable.getValue().getPageSize());
    }

    @Test
    void returnsEmptyListWhenNoMatch() {
        when(repository.findByNameContainingOrderByNameAsc(anyString(), any(Pageable.class))).thenReturn(List.of());

        assertEquals(List.of(), service.search("zzz"));
    }

    @Test
    void passesWildcardCharactersAsIs() {
        when(repository.findByNameContainingOrderByNameAsc(anyString(), any(Pageable.class))).thenReturn(List.of());

        service.search("100%_");

        verify(repository).findByNameContainingOrderByNameAsc(eq("100%_"), any(Pageable.class));
    }

    @Test
    void rejectsBlankOrMissingKeyword() {
        for (String keyword : new String[]{null, "", "   "}) {
            CustomException e = assertThrows(CustomException.class, () -> service.search(keyword));
            assertEquals(ErrorCode.INVALID_INPUT, e.getErrorCode());
            assertEquals("입력값이 올바르지 않습니다", e.getMessage());
            assertEquals("keyword", e.getErrors().getFirst().field());
        }
        verifyNoInteractions(repository);
    }

    @Test
    void keywordLengthLimitIsFiftyAfterTrim() {
        when(repository.findByNameContainingOrderByNameAsc(anyString(), any(Pageable.class))).thenReturn(List.of());

        service.search(" " + "a".repeat(50) + " ");

        CustomException e = assertThrows(CustomException.class, () -> service.search("a".repeat(51)));
        assertEquals("keyword", e.getErrors().getFirst().field());
    }

    @Test
    void wrapsRepositoryFailureAs500() {
        when(repository.findByNameContainingOrderByNameAsc(anyString(), any(Pageable.class)))
                .thenThrow(new IllegalStateException("db down"));

        CustomException e = assertThrows(CustomException.class, () -> service.search("nus"));

        assertEquals(ErrorCode.INTERNAL_ERROR, e.getErrorCode());
        assertEquals("학교 검색 중 오류가 발생했습니다.", e.getMessage());
    }
}
