package com.likelion14th._Team.domain.university;

import com.likelion14th._Team.domain.university.controller.UniversityCourseController;
import com.likelion14th._Team.domain.university.repository.UniversityCourseRepository;
import com.likelion14th._Team.domain.university.repository.UniversityRepository;
import com.likelion14th._Team.domain.university.service.UniversityCourseService;
import com.likelion14th._Team.global.security.ActiveUserTestConfig;
import com.likelion14th._Team.global.security.JwtProvider;
import com.likelion14th._Team.global.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UniversityCourseController.class,
        properties = "app.cors.allowed-origins=http://localhost:3000")
@Import({SecurityConfig.class, ActiveUserTestConfig.class, UniversityCourseService.class})
class UniversityCourseFailureTests {
    @Autowired MockMvc mvc;
    @MockitoBean JwtProvider jwt;
    @MockitoBean UniversityRepository universities;
    @MockitoBean UniversityCourseRepository courses;

    @Test
    void repositoryFailureThroughRealServiceReturnsApi500() throws Exception {
        when(jwt.getUserId("valid")).thenReturn(1L);
        when(universities.existsById(4L)).thenReturn(true);
        when(courses.courses(4L))
                .thenThrow(new DataAccessResourceFailureException("test database unavailable"));

        mvc.perform(get("/api/v1/universities/4/courses")
                        .header("Authorization", "Bearer valid"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("E500"))
                .andExpect(jsonPath("$.message").value("수강 과목 목록 조회 중 오류가 발생했습니다"))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
        verify(courses).courses(4L);
    }
}
