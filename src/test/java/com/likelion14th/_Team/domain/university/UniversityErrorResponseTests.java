package com.likelion14th._Team.domain.university;

import com.likelion14th._Team.domain.university.controller.UniversityController;
import com.likelion14th._Team.domain.university.controller.UniversityCourseController;
import com.likelion14th._Team.domain.university.controller.UniversityReturnReportController;
import com.likelion14th._Team.domain.university.controller.UniversityCourseCaseController;
import com.likelion14th._Team.domain.university.service.UniversityQueryService;
import com.likelion14th._Team.domain.university.service.UniversityService;
import com.likelion14th._Team.domain.university.service.UniversityCourseService;
import com.likelion14th._Team.domain.university.service.UniversityReturnReportService;
import com.likelion14th._Team.domain.university.service.UniversityCourseCaseService;
import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import com.likelion14th._Team.global.response.FieldErrorDetail;
import com.likelion14th._Team.global.security.JwtProvider;
import com.likelion14th._Team.global.security.ActiveUserTestConfig;
import com.likelion14th._Team.global.security.SecurityConfig;
import io.jsonwebtoken.JwtException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {UniversityController.class, UniversityCourseController.class,
        UniversityReturnReportController.class, UniversityCourseCaseController.class},
        properties = "app.cors.allowed-origins=http://localhost:3000")
@Import({SecurityConfig.class, ActiveUserTestConfig.class})
class UniversityErrorResponseTests {
    @Autowired MockMvc mvc;
    @MockitoBean UniversityQueryService queries;
    @MockitoBean UniversityService search;
    @MockitoBean UniversityCourseService courses;
    @MockitoBean UniversityReturnReportService reports;
    @MockitoBean UniversityCourseCaseService cases;
    @MockitoBean JwtProvider jwt;

    private static final String BASE = "/api/v1/universities";
    private static final List<String> SUFFIXES = List.of("", "/courses", "/return-reports", "/course-cases");

    @BeforeEach
    void authenticate() {
        when(jwt.getUserId("valid")).thenReturn(1L);
        when(jwt.getUserId("invalid")).thenThrow(new JwtException("invalid"));
        when(jwt.getUserId("expired")).thenThrow(new JwtException("expired"));
    }

    private ResultActions request(String path) throws Exception {
        return mvc.perform(get(path).header("Authorization", "Bearer valid"));
    }

    @Test
    void allIdRoutesKeepExact400Response() throws Exception {
        for (String suffix : SUFFIXES) {
            for (String id : List.of("0", "-1", "abc", "1.5", "9223372036854775808")) {
                request(BASE + "/" + id + suffix)
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value("E400"))
                        .andExpect(jsonPath("$.message").value("입력값이 올바르지 않습니다"))
                        .andExpect(jsonPath("$.data").value(nullValue()))
                        .andExpect(jsonPath("$.errors.length()").value(1))
                        .andExpect(jsonPath("$.errors[0].field").value("universityId"))
                        .andExpect(jsonPath("$.errors[0].reason").value("양의 정수를 입력해 주세요."));
            }
        }
        verifyNoInteractions(queries, courses, reports, cases);
    }

    @Test
    void authenticationStillPrecedesIdValidation() throws Exception {
        for (String suffix : SUFFIXES) {
            String path = BASE + "/abc" + suffix;
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
            for (String token : List.of("invalid", "expired")) {
                mvc.perform(get(path).header("Authorization", "Bearer " + token))
                        .andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.code").value("E401"))
                        .andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
            }
        }
        verifyNoInteractions(queries, courses, reports, cases);
    }

    @Test
    void allSixUnexpectedFailuresKeepTheirApiMessages() throws Exception {
        var failure = new DataAccessResourceFailureException("test database unavailable");
        when(queries.list(eq(1L), any())).thenThrow(failure);
        when(queries.countries(1L)).thenThrow(failure);
        when(queries.detail(eq(1L), eq(4L), isNull())).thenThrow(failure);
        when(courses.courses(1L, 4L)).thenThrow(failure);
        when(reports.reports(4L)).thenThrow(failure);
        when(cases.cases(4L)).thenThrow(failure);
        String[][] expected = {
                {BASE, "협정교 목록 조회 중 오류가 발생했습니다."},
                {BASE + "/countries", "국가 목록 조회 중 오류가 발생했습니다"},
                {BASE + "/4", "협정교 상세 조회 중 오류가 발생했습니다"},
                {BASE + "/4/courses", "수강 과목 목록 조회 중 오류가 발생했습니다"},
                {BASE + "/4/return-reports", "귀국보고서 조회 중 오류가 발생했습니다"},
                {BASE + "/4/course-cases", "인정 사례 목록 조회 중 오류가 발생했습니다"}
        };
        for (String[] endpoint : expected) {
            request(endpoint[0]).andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("E500"))
                    .andExpect(jsonPath("$.message").value(endpoint[1]))
                    .andExpect(jsonPath("$.data").value(nullValue()))
                    .andExpect(jsonPath("$.errors").value(nullValue()));
        }
    }

    @Test
    void custom404And500AreNotOverwrittenByApiFailure() throws Exception {
        var notFound = new CustomException(ErrorCode.UNIVERSITY_NOT_FOUND, "존재하지 않는 학교입니다");
        when(queries.detail(eq(1L), eq(4L), isNull())).thenThrow(notFound);
        when(courses.courses(1L, 4L)).thenThrow(notFound);
        when(reports.reports(4L)).thenThrow(notFound);
        when(cases.cases(4L)).thenThrow(notFound);
        for (String suffix : SUFFIXES) {
            request(BASE + "/4" + suffix).andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("E404"))
                    .andExpect(jsonPath("$.message").value("존재하지 않는 학교입니다"));
        }
        doThrow(new CustomException(ErrorCode.INTERNAL_ERROR,
                "서비스 오류 메시지", List.of(new FieldErrorDetail("sample", "원래 오류"))))
                .when(cases).cases(4L);
        request(BASE + "/4/course-cases").andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("서비스 오류 메시지"))
                .andExpect(jsonPath("$.errors[0].reason").value("원래 오류"));
    }

    @Test
    void framework405AndUnannotatedEndpointKeepGlobalBehavior() throws Exception {
        mvc.perform(post(BASE + "/4/course-cases").header("Authorization", "Bearer valid"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("E405"));
        when(search.search("test")).thenThrow(new IllegalStateException("unexpected"));
        request(BASE + "/search?keyword=test").andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value(ErrorCode.INTERNAL_ERROR.getMessage()));
    }
}
