package com.likelion14th._Team.domain.university.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.likelion14th._Team.domain.university.service.UniversityCourseService;
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

@WebMvcTest(controllers = UniversityCourseController.class,
        properties = "app.cors.allowed-origins=http://localhost:3000")
@Import({SecurityConfig.class, ActiveUserTestConfig.class})
@ExtendWith(OutputCaptureExtension.class)
class UniversityCourseControllerLoggingTests {
    @Autowired MockMvc mvc;
    @MockitoBean UniversityCourseService service;
    @MockitoBean JwtProvider jwt;

    @Test
    void coursesFailureDoesNotLogExceptionMessage(CapturedOutput output) throws Exception {
        String marker = UUID.randomUUID().toString();
        when(jwt.getUserId(marker)).thenReturn(1L);
        when(service.courses(1L, 4L))
                .thenThrow(new IllegalStateException(marker, new IllegalArgumentException(marker)));

        mvc.perform(get("/api/v1/universities/4/courses").header("Authorization", "Bearer " + marker))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("E500"));

        assertFalse(output.getAll().contains(marker));
        assertTrue(output.getAll().contains("at " + getClass().getName()));
        assertTrue(output.getAll().contains("Caused by:"));
        assertTrue(output.getAll().contains("java.lang.IllegalArgumentException"));
    }
}