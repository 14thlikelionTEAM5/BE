package com.likelion14th._Team.domain.admission;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.likelion14th._Team.domain.admission.controller.AdmissionReportController;
import com.likelion14th._Team.domain.admission.service.AdmissionReportStatsService;
import com.likelion14th._Team.global.security.ActiveUserTestConfig;
import com.likelion14th._Team.global.security.JwtProvider;
import com.likelion14th._Team.global.security.SecurityConfig;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdmissionReportController.class,
        properties = "app.cors.allowed-origins=http://localhost:3000")
@Import({SecurityConfig.class, ActiveUserTestConfig.class})
@ExtendWith(OutputCaptureExtension.class)
class AdmissionReportControllerLoggingTests {
    private static final String URL = "/api/v1/universities/4/admission-reports";

    @Autowired MockMvc mvc;
    @MockitoBean AdmissionReportStatsService service;
    @MockitoBean JwtProvider jwt;

    @Test
    void listFailureDoesNotLogExceptionMessage(CapturedOutput output) throws Exception {
        String marker = UUID.randomUUID().toString();
        when(jwt.getUserId(marker)).thenReturn(1L);
        when(service.list(1L, 4L, null))
                .thenThrow(new IllegalStateException(marker, new IllegalArgumentException(marker)));

        mvc.perform(get(URL).header("Authorization", "Bearer " + marker))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("E500"));

        assertLogHasStackButNoMarker(output, marker);
    }

    @Test
    void statsFailureDoesNotLogExceptionMessage(CapturedOutput output) throws Exception {
        String marker = UUID.randomUUID().toString();
        when(jwt.getUserId(marker)).thenReturn(1L);
        when(service.stats(eq(1L), any()))
                .thenThrow(new IllegalStateException(marker, new IllegalArgumentException(marker)));

        mvc.perform(get(URL + "/stats").header("Authorization", "Bearer " + marker))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("E500"));

        assertLogHasStackButNoMarker(output, marker);
    }

    private void assertLogHasStackButNoMarker(CapturedOutput output, String marker) {
        assertFalse(output.getAll().contains(marker));
        assertTrue(output.getAll().contains("at " + getClass().getName()));
        assertTrue(output.getAll().contains("Caused by:"));
        assertTrue(output.getAll().contains("java.lang.IllegalArgumentException"));
    }
}