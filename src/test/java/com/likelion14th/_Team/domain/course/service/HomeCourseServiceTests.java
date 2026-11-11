package com.likelion14th._Team.domain.course.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.likelion14th._Team.domain.course.dto.HomeCourseSearchResponse;
import com.likelion14th._Team.domain.course.entity.HomeCourse;
import com.likelion14th._Team.domain.course.repository.HomeCourseRepository;
import com.likelion14th._Team.global.enums.CourseCategory;
import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import com.likelion14th._Team.global.response.FieldErrorDetail;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

class HomeCourseServiceTests {

    private final HomeCourseRepository repository = mock(HomeCourseRepository.class);
    private final HomeCourseService service = new HomeCourseService(repository);

    @Test
    void searchesByCategoryWithTrimmedKeywordAndLimitTen() {
        HomeCourse course = HomeCourse.builder().code("BUS2101").name("마케팅원론").credit(new BigDecimal("3.0"))
                .category(CourseCategory.MAJOR_ELECTIVE).baseYear("2025-2026").build();
        when(repository.findByCategoryAndIsBusinessOfferedTrueAndNameContainingOrderByNameAsc(
                eq(CourseCategory.MAJOR_ELECTIVE), eq("마케팅"), any(Pageable.class))).thenReturn(List.of(course));

        List<HomeCourseSearchResponse> result = service.search(" 마케팅 ", "MAJOR_ELECTIVE");

        assertEquals(1, result.size());
        assertEquals("BUS2101", result.getFirst().code());
        assertEquals(new BigDecimal("3.0"), result.getFirst().credit());
        assertEquals(CourseCategory.MAJOR_ELECTIVE, result.getFirst().category());
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findByCategoryAndIsBusinessOfferedTrueAndNameContainingOrderByNameAsc(
                eq(CourseCategory.MAJOR_ELECTIVE), eq("마케팅"), pageable.capture());
        assertEquals(10, pageable.getValue().getPageSize());
    }

    @Test
    void acceptsMajorRequired() {
        when(repository.findByCategoryAndIsBusinessOfferedTrueAndNameContainingOrderByNameAsc(
                any(), anyString(), any(Pageable.class))).thenReturn(List.of());

        assertEquals(List.of(), service.search("회계", "MAJOR_REQUIRED"));

        verify(repository).findByCategoryAndIsBusinessOfferedTrueAndNameContainingOrderByNameAsc(
                eq(CourseCategory.MAJOR_REQUIRED), eq("회계"), any(Pageable.class));
    }

    @Test
    void rejectsGeneralAndUnknownRecognitionTypes() {
        for (String type : new String[]{"GENERAL", "LIBERAL_ARTS", "major_elective", "MAJOR_ELECTIVE ", "", null}) {
            CustomException e = assertThrows(CustomException.class, () -> service.search("마케팅", type));
            assertEquals(ErrorCode.INVALID_INPUT, e.getErrorCode());
            assertEquals(List.of("recognitionType"), e.getErrors().stream().map(FieldErrorDetail::field).toList());
        }
        verifyNoInteractions(repository);
    }

    @Test
    void reportsBothFieldErrorsTogether() {
        CustomException e = assertThrows(CustomException.class, () -> service.search(" ", "GENERAL"));

        assertEquals(List.of("keyword", "recognitionType"),
                e.getErrors().stream().map(FieldErrorDetail::field).toList());
    }

    @Test
    void wrapsRepositoryFailureAs500() {
        when(repository.findByCategoryAndIsBusinessOfferedTrueAndNameContainingOrderByNameAsc(
                any(), anyString(), any(Pageable.class))).thenThrow(new IllegalStateException("db down"));

        CustomException e = assertThrows(CustomException.class, () -> service.search("마케팅", "MAJOR_ELECTIVE"));

        assertEquals(ErrorCode.INTERNAL_ERROR, e.getErrorCode());
        assertEquals("본교 과목 검색 중 오류가 발생했습니다.", e.getMessage());
    }
}
