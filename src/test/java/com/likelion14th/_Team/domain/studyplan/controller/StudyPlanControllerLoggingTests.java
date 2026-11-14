package com.likelion14th._Team.domain.studyplan.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.likelion14th._Team.domain.studyplan.service.StudyPlanItemService;
import com.likelion14th._Team.domain.studyplan.service.StudyPlanService;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = StudyPlanController.class,
        properties = "app.cors.allowed-origins=http://localhost:3000")
@Import({SecurityConfig.class, ActiveUserTestConfig.class})
@ExtendWith(OutputCaptureExtension.class)
class StudyPlanControllerLoggingTests {
    private static final String BASE = "/api/v1/study-plans";

    @Autowired MockMvc mvc;
    @MockitoBean StudyPlanService studyPlanService;
    @MockitoBean StudyPlanItemService studyPlanItemService;
    @MockitoBean JwtProvider jwt;

    private IllegalStateException boom(String marker) {
        return new IllegalStateException(marker, new IllegalArgumentException(marker));
    }

    @Test
    void deleteItemFailureDoesNotLogExceptionMessage(CapturedOutput output) throws Exception {
        String marker = UUID.randomUUID().toString();
        when(jwt.getUserId(marker)).thenReturn(1L);
        when(studyPlanItemService.delete(1L, 4L, 5L)).thenThrow(boom(marker));

        mvc.perform(delete(BASE + "/4/items/5").header("Authorization", "Bearer " + marker))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("E500"));

        assertLogHasStackButNoMarker(output, marker);
    }

    @Test
    void updateItemFailureDoesNotLogExceptionMessage(CapturedOutput output) throws Exception {
        String marker = UUID.randomUUID().toString();
        when(jwt.getUserId(marker)).thenReturn(1L);
        when(studyPlanItemService.update(eq(1L), eq(4L), eq(5L), any())).thenThrow(boom(marker));

        mvc.perform(patch(BASE + "/4/items/5").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + marker).content("{}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("E500"));

        assertLogHasStackButNoMarker(output, marker);
    }

    @Test
    void createItemFailureDoesNotLogExceptionMessage(CapturedOutput output) throws Exception {
        String marker = UUID.randomUUID().toString();
        when(jwt.getUserId(marker)).thenReturn(1L);
        when(studyPlanItemService.create(eq(1L), eq(4L), any())).thenThrow(boom(marker));

        mvc.perform(post(BASE + "/4/items").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + marker).content("{}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("E500"));

        assertLogHasStackButNoMarker(output, marker);
    }

    @Test
    void detailFailureDoesNotLogExceptionMessage(CapturedOutput output) throws Exception {
        String marker = UUID.randomUUID().toString();
        when(jwt.getUserId(marker)).thenReturn(1L);
        when(studyPlanService.detail(1L, 4L)).thenThrow(boom(marker));

        mvc.perform(get(BASE + "/4").header("Authorization", "Bearer " + marker))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("E500"));

        assertLogHasStackButNoMarker(output, marker);
    }

    @Test
    void renameFailureDoesNotLogExceptionMessage(CapturedOutput output) throws Exception {
        String marker = UUID.randomUUID().toString();
        when(jwt.getUserId(marker)).thenReturn(1L);
        when(studyPlanService.rename(eq(1L), eq(4L), any())).thenThrow(boom(marker));

        mvc.perform(patch(BASE + "/4").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + marker).content("{}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("E500"));

        assertLogHasStackButNoMarker(output, marker);
    }

    @Test
    void deleteFailureDoesNotLogExceptionMessage(CapturedOutput output) throws Exception {
        String marker = UUID.randomUUID().toString();
        when(jwt.getUserId(marker)).thenReturn(1L);
        when(studyPlanService.delete(1L, 4L)).thenThrow(boom(marker));

        mvc.perform(delete(BASE + "/4").header("Authorization", "Bearer " + marker))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("E500"));

        assertLogHasStackButNoMarker(output, marker);
    }

    @Test
    void listFailureHandledByFallbackDoesNotLogExceptionMessage(CapturedOutput output) throws Exception {
        String marker = UUID.randomUUID().toString();
        when(jwt.getUserId(marker)).thenReturn(1L);
        when(studyPlanService.list(1L)).thenThrow(boom(marker));

        mvc.perform(get(BASE).header("Authorization", "Bearer " + marker))
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