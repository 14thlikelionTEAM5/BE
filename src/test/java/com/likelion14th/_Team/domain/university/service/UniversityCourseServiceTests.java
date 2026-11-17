package com.likelion14th._Team.domain.university.service;

import com.likelion14th._Team.domain.university.dto.UniversityCoursesResponse;
import com.likelion14th._Team.domain.university.repository.UniversityCourseRepository;
import com.likelion14th._Team.domain.university.repository.UniversityRepository;
import com.likelion14th._Team.domain.university.repository.projection.UniversityCourseRow;
import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UniversityCourseServiceTests {
    private final UniversityRepository universities = mock(UniversityRepository.class);
    private final UniversityCourseRepository repository = mock(UniversityCourseRepository.class);
    private final UniversityCourseService service = new UniversityCourseService(universities, repository);

    @Test
    void convertsProjectionToResponseWithoutChangingValuesOrOrder() {
        when(universities.existsById(4L)).thenReturn(true);
        when(repository.courses(4L)).thenReturn(List.of(
                new UniversityCourseRow(12L, null, "Alpha", new BigDecimal("2.5"), false),
                new UniversityCourseRow(9L, "Z001", "Zulu", new BigDecimal("5.0"), true)));
        var response = service.courses(1L, 4L);
        assertEquals(4L, response.universityId());
        assertEquals(List.of(
                new UniversityCoursesResponse.Course(12L, null, "Alpha", new BigDecimal("2.5"), false),
                new UniversityCoursesResponse.Course(9L, "Z001", "Zulu", new BigDecimal("5.0"), true)), response.courses());
    }

    @Test
    void authenticationAndSchoolExistenceChecksStillPrecedeCourseQuery() {
        assertEquals(ErrorCode.UNAUTHORIZED,
                assertThrows(CustomException.class, () -> service.courses(null, 4L)).getErrorCode());
        verifyNoInteractions(universities, repository);
        var missing = assertThrows(CustomException.class, () -> service.courses(1L, 4L));
        assertEquals(ErrorCode.UNIVERSITY_NOT_FOUND, missing.getErrorCode());
        assertEquals("존재하지 않는 학교입니다", missing.getMessage());
        verifyNoInteractions(repository);
    }
}
