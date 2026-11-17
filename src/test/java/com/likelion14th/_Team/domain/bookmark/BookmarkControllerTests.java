package com.likelion14th._Team.domain.bookmark;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.likelion14th._Team.domain.bookmark.controller.BookmarkController;
import com.likelion14th._Team.domain.bookmark.service.BookmarkService;
import com.likelion14th._Team.global.security.JwtProvider;
import com.likelion14th._Team.global.security.SecurityConfig;
import com.likelion14th._Team.global.security.ActiveUserTestConfig;
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

@WebMvcTest(controllers = BookmarkController.class,
        properties = "app.cors.allowed-origins=http://localhost:3000")
@Import({SecurityConfig.class, ActiveUserTestConfig.class})
@ExtendWith(OutputCaptureExtension.class)
class BookmarkControllerTests {
    @Autowired MockMvc mvc;
    @MockitoBean BookmarkService service;
    @MockitoBean JwtProvider jwt;

    @Test
    void unexpectedFailureIs500WithoutLoggingRequestOrExceptionDetails(CapturedOutput output) throws Exception {
        String marker = UUID.randomUUID().toString();
        when(jwt.getUserId(marker)).thenReturn(1L);
        when(service.create(1L, 4L)).thenThrow(new IllegalStateException(marker, new IllegalArgumentException(marker)));
        mvc.perform(post("/api/v1/bookmarks").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + marker)
                        .content("{\"universityId\":4,\"password\":\"" + marker + "\",\"authCode\":\"" + marker + "\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("E500"))
                .andExpect(jsonPath("$.message").value("찜 추가 중 오류가 발생했습니다"))
                .andExpect(jsonPath("$.data").doesNotExist());
        assertFalse(output.getAll().contains(marker));
        assertTrue(output.getAll().contains("at " + getClass().getName()));
        assertTrue(output.getAll().contains("Caused by:"));
        assertTrue(output.getAll().contains("java.lang.IllegalArgumentException"));
    }
    @Test
    void deleteFailureIs500WithoutLoggingExceptionDetails(CapturedOutput output) throws Exception {
        String marker = UUID.randomUUID().toString();
        when(jwt.getUserId(marker)).thenReturn(1L);
        when(service.delete(1L, 4L)).thenThrow(new IllegalStateException(marker, new IllegalArgumentException(marker)));
        mvc.perform(delete("/api/v1/bookmarks/4").header("Authorization", "Bearer " + marker))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("E500"))
                .andExpect(jsonPath("$.message").value("찜 해제 중 오류가 발생했습니다"))
                .andExpect(jsonPath("$.data").doesNotExist());
        assertFalse(output.getAll().contains(marker));
        assertTrue(output.getAll().contains("at " + getClass().getName()));
        assertTrue(output.getAll().contains("Caused by:"));
        assertTrue(output.getAll().contains("java.lang.IllegalArgumentException"));
    }

    @Test
    void listFailureIs500WithoutLoggingExceptionDetails(CapturedOutput output) throws Exception {
        String marker = UUID.randomUUID().toString();
        when(jwt.getUserId(marker)).thenReturn(1L);
        when(service.list(1L)).thenThrow(new IllegalStateException(marker, new IllegalArgumentException(marker)));
        mvc.perform(get("/api/v1/bookmarks").header("Authorization", "Bearer " + marker))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("E500"))
                .andExpect(jsonPath("$.message").value("찜 목록 조회 중 오류가 발생했습니다"));
        assertFalse(output.getAll().contains(marker));
        assertTrue(output.getAll().contains("at " + getClass().getName()));
        assertTrue(output.getAll().contains("Caused by:"));
        assertTrue(output.getAll().contains("java.lang.IllegalArgumentException"));
    }

}
