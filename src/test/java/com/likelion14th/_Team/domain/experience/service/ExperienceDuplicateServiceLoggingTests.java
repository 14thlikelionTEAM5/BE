package com.likelion14th._Team.domain.experience.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.likelion14th._Team.domain.admission.repository.AdmissionReportRepository;
import com.likelion14th._Team.domain.returnreport.repository.ReturnReportCourseRepository;
import com.likelion14th._Team.domain.returnreport.repository.ReturnReportRepository;
import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class ExperienceDuplicateServiceLoggingTests {
    private final ReturnReportRepository returnReportRepository = mock(ReturnReportRepository.class);
    private final ExperienceDuplicateService service = new ExperienceDuplicateService(
            returnReportRepository,
            mock(ReturnReportCourseRepository.class),
            mock(AdmissionReportRepository.class));

    @Test
    void checkFailureDoesNotLogExceptionMessage(CapturedOutput output) {
        String marker = UUID.randomUUID().toString();
        when(returnReportRepository.existsByUserIdAndSemester(1L, "2026-1"))
                .thenThrow(new IllegalStateException(marker, new IllegalArgumentException(marker)));

        CustomException thrown = assertThrows(CustomException.class,
                () -> service.check(1L, "RETURN_REPORT", 4L, "2026-1"));

        assertEquals(ErrorCode.INTERNAL_ERROR, thrown.getErrorCode());
        assertFalse(output.getAll().contains(marker));
        assertTrue(output.getAll().contains("at " + getClass().getName()));
        assertTrue(output.getAll().contains("Caused by:"));
        assertTrue(output.getAll().contains("java.lang.IllegalArgumentException"));
    }
}