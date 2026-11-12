package com.likelion14th._Team.domain.university;

import com.likelion14th._Team.domain.returnreport.repository.ReturnReportRepository;
import com.likelion14th._Team.domain.university.controller.UniversityReturnReportController;
import com.likelion14th._Team.domain.university.repository.UniversityRepository;
import com.likelion14th._Team.domain.university.service.UniversityReturnReportService;
import com.likelion14th._Team.global.security.JwtProvider;
import com.likelion14th._Team.global.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UniversityReturnReportController.class,
        properties = "app.cors.allowed-origins=http://localhost:3000")
@Import({SecurityConfig.class, UniversityReturnReportService.class})
class UniversityReturnReportFailureTests {
    @Autowired MockMvc mvc;
    @MockitoBean JwtProvider jwt;
    @MockitoBean UniversityRepository universities;
    @MockitoBean ReturnReportRepository reports;

    @Test
    void repositoryFailureThroughRealServiceReturnsApi500() throws Exception {
        when(jwt.getUserId("valid")).thenReturn(1L);
        when(universities.existsById(4L)).thenReturn(true);
        when(reports.findByUniversityIdOrderBySemesterDescCreatedAtDescIdDesc(4L))
                .thenThrow(new DataAccessResourceFailureException("test database unavailable"));

        mvc.perform(get("/api/v1/universities/4/return-reports")
                        .header("Authorization", "Bearer valid"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("E500"))
                .andExpect(jsonPath("$.message").value("귀국보고서 조회 중 오류가 발생했습니다"))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
        verify(reports).findByUniversityIdOrderBySemesterDescCreatedAtDescIdDesc(4L);
    }
}
