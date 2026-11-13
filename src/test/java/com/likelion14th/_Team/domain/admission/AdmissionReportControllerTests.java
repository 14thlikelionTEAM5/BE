package com.likelion14th._Team.domain.admission;

import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.likelion14th._Team.domain.admission.controller.AdmissionReportController;
import com.likelion14th._Team.domain.admission.dto.AdmissionReportListResponse;
import com.likelion14th._Team.domain.admission.service.AdmissionReportStatsService;
import com.likelion14th._Team.global.enums.AdmissionResult;
import com.likelion14th._Team.global.security.JwtProvider;
import com.likelion14th._Team.global.security.SecurityConfig;
import com.likelion14th._Team.global.security.ActiveUserTestConfig;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdmissionReportController.class,
        properties = "app.cors.allowed-origins=http://localhost:3000")
@Import({SecurityConfig.class, ActiveUserTestConfig.class})
class AdmissionReportControllerTests {
    private static final String URL = "/api/v1/universities/4/admission-reports";

    @Autowired
    MockMvc mvc;
    @MockitoBean
    AdmissionReportStatsService service;
    @MockitoBean
    JwtProvider jwt;

    @BeforeEach
    void setUp() {
        when(jwt.getUserId("test-token")).thenReturn(1L);
    }

    @Test
    void allPassAndFailAreParsedBeforeCallingService() throws Exception {
        for (String value : List.of("ALL", "PASS", "FAIL")) {
            AdmissionResult expected = value.equals("ALL") ? null : AdmissionResult.valueOf(value);
            when(service.list(1L, 4L, expected))
                    .thenReturn(new AdmissionReportListResponse(4L, 0, List.of()));
            mvc.perform(get(URL).param("result", value).header("Authorization", "Bearer test-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("S200"))
                    .andExpect(jsonPath("$.data.admissionReports").isEmpty());
            verify(service).list(1L, 4L, expected);
            clearInvocations(service);
        }
    }

    @Test
    void missingResultUsesAllCases() throws Exception {
        when(service.list(1L, 4L, null))
                .thenReturn(new AdmissionReportListResponse(4L, 0, List.of()));
        mvc.perform(get(URL).header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk());
        verify(service).list(1L, 4L, null);
    }

    @Test
    void invalidResultsReturn400WithFieldErrorWithoutCallingService() throws Exception {
        for (String value : List.of("WRONG", "pass", "", " ")) {
            mvc.perform(get(URL).param("result", value).header("Authorization", "Bearer test-token"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("E400"))
                    .andExpect(jsonPath("$.data").doesNotExist())
                    .andExpect(jsonPath("$.errors[0].field").value("result"));
        }
        verifyNoInteractions(service);
    }
}
