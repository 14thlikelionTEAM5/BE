package com.likelion14th._Team.domain.course.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.likelion14th._Team.domain.course.repository.HomeCourseRepository;
import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class HomeCourseServiceLoggingTests {
    private final HomeCourseRepository homeCourseRepository = mock(HomeCourseRepository.class);
    private final HomeCourseService service = new HomeCourseService(homeCourseRepository);

    @Test
    void searchFailureDoesNotLogExceptionMessage(CapturedOutput output) {
        String marker = UUID.randomUUID().toString();
        when(homeCourseRepository.findByCategoryAndIsBusinessOfferedTrueAndNameContainingOrderByNameAsc(
                any(), any(), any()))
                .thenThrow(new IllegalStateException(marker, new IllegalArgumentException(marker)));

        CustomException thrown = assertThrows(CustomException.class,
                () -> service.search("경영", "MAJOR_REQUIRED"));

        assertEquals(ErrorCode.INTERNAL_ERROR, thrown.getErrorCode());
        assertFalse(output.getAll().contains(marker));
        assertTrue(output.getAll().contains("at " + getClass().getName()));
        assertTrue(output.getAll().contains("Caused by:"));
        assertTrue(output.getAll().contains("java.lang.IllegalArgumentException"));
    }
}