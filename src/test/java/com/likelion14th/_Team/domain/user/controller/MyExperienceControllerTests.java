package com.likelion14th._Team.domain.user.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.likelion14th._Team.domain.user.dto.MyExperiencesResponse;
import com.likelion14th._Team.domain.user.dto.MyExperiencesResponse.AdmissionReportItem;
import com.likelion14th._Team.domain.user.dto.MyExperiencesResponse.CourseCaseItem;
import com.likelion14th._Team.domain.user.dto.MyExperiencesResponse.ReturnReportItem;
import com.likelion14th._Team.domain.user.service.MyExperienceService;
import com.likelion14th._Team.global.enums.AdmissionResult;
import com.likelion14th._Team.global.enums.CourseCategory;
import com.likelion14th._Team.global.enums.LanguageTestType;
import com.likelion14th._Team.global.enums.MajorType;
import com.likelion14th._Team.global.security.JwtProvider;
import com.likelion14th._Team.global.security.SecurityConfig;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = MyExperienceController.class,
        properties = "app.cors.allowed-origins=http://localhost:3000")
@Import(SecurityConfig.class)
class MyExperienceControllerTests {
    private static final String URL = "/api/v1/users/me/experiences";

    @Autowired
    MockMvc mvc;
    @MockitoBean
    MyExperienceService service;
    @MockitoBean
    JwtProvider jwt;

    @BeforeEach
    void setUp() {
        when(jwt.getUserId("test-token")).thenReturn(1L);
    }

    @Test
    void returnsAllThreeKindsOfExperiences() throws Exception {
        var admission = new AdmissionReportItem(8L, 12L, "National University of Singapore", "2027-1",
                AdmissionResult.PASS, LanguageTestType.TOEFL_IBT, new BigDecimal("100.0"),
                new BigDecimal("4.12"), MajorType.PRIMARY);
        var report = new ReturnReportItem(5L, 12L, "National University of Singapore", "2026-2",
                new ReturnReportItem.Cost("왕복 약 90만원", "약 30만원", "월 150만원", "약 1,200만원"),
                "DORM",
                new ReturnReportItem.Visa("약 2주", "Student Pass"),
                "삼성화재",
                List.of(new ReturnReportItem.Course(21L, "MKT2101", "Principles of Marketing",
                        new BigDecimal("4.0"), 4)),
                "기숙사 생활이 편리했다.");
        var courseCase = new CourseCaseItem(14L, 12L, "National University of Singapore", "2026-2",
                21L, "Principles of Marketing", CourseCategory.MAJOR_ELECTIVE,
                new CourseCaseItem.HomeCourseSummary(305L, "BUS2101", "마케팅원론", new BigDecimal("3.0")),
                new BigDecimal("3.0"), MajorType.PRIMARY);
        when(service.getMyExperiences(1L))
                .thenReturn(new MyExperiencesResponse(List.of(admission), List.of(report), List.of(courseCase)));

        mvc.perform(get(URL).header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("S200"))
                .andExpect(jsonPath("$.data.admissionReports[0].admissionReportId").value(8))
                .andExpect(jsonPath("$.data.admissionReports[0].universityName")
                        .value("National University of Singapore"))
                .andExpect(jsonPath("$.data.returnReports[0].cost.flight").value("왕복 약 90만원"))
                .andExpect(jsonPath("$.data.returnReports[0].visa.type").value("Student Pass"))
                .andExpect(jsonPath("$.data.returnReports[0].courses[0].courseId").value(21))
                .andExpect(jsonPath("$.data.returnReports[0].courses[0].satisfaction").value(4))
                .andExpect(jsonPath("$.data.courseCases[0].returnReportCourseId").value(21))
                .andExpect(jsonPath("$.data.courseCases[0].homeCourse.code").value("BUS2101"));
        verify(service).getMyExperiences(1L);
    }

    @Test
    void generalCourseCaseHasNullHomeCourse() throws Exception {
        var courseCase = new CourseCaseItem(15L, 12L, "National University of Singapore", "2026-2",
                22L, "Public Speaking", CourseCategory.GENERAL, null,
                new BigDecimal("3.0"), MajorType.PRIMARY);
        when(service.getMyExperiences(1L))
                .thenReturn(new MyExperiencesResponse(List.of(), List.of(), List.of(courseCase)));

        mvc.perform(get(URL).header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courseCases[0].homeCourse").isEmpty());
    }

    @Test
    void emptyExperiencesReturnEmptyArrays() throws Exception {
        when(service.getMyExperiences(1L))
                .thenReturn(new MyExperiencesResponse(List.of(), List.of(), List.of()));

        mvc.perform(get(URL).header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.admissionReports").isEmpty())
                .andExpect(jsonPath("$.data.returnReports").isEmpty())
                .andExpect(jsonPath("$.data.courseCases").isEmpty());
    }

    @Test
    void withoutTokenReturns401WithoutCallingService() throws Exception {
        mvc.perform(get(URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("E401"));
        verifyNoInteractions(service);
    }
}