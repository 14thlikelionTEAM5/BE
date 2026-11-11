package com.likelion14th._Team.domain.university.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.likelion14th._Team.domain.university.dto.UniversitySearchResponse;
import com.likelion14th._Team.domain.university.service.UniversityService;
import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import com.likelion14th._Team.global.response.FieldErrorDetail;
import com.likelion14th._Team.global.security.JwtProvider;
import com.likelion14th._Team.global.security.SecurityConfig;
import io.jsonwebtoken.JwtException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = UniversityController.class, properties = "app.cors.allowed-origins=http://localhost:3000")
@Import(SecurityConfig.class)
class UniversityControllerTests {

    private static final String URL = "/api/v1/universities/search";
    private static final String BEARER = "Bearer valid-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UniversityService universityService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        when(jwtProvider.getUserId("valid-token")).thenReturn(1L);
    }

    @Test
    void returns401InCommonFormatWithoutToken() throws Exception {
        mockMvc.perform(get(URL).param("keyword", "nus"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("E401"))
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
        verifyNoInteractions(universityService);
    }

    @Test
    void returns401WithInvalidToken() throws Exception {
        when(jwtProvider.getUserId("broken")).thenThrow(new JwtException("invalid"));

        mockMvc.perform(get(URL).param("keyword", "nus").header(HttpHeaders.AUTHORIZATION, "Bearer broken"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("E401"));
    }

    @Test
    void searchReturnsUniversities() throws Exception {
        when(universityService.search("Nan")).thenReturn(List.of(
                new UniversitySearchResponse(31L, "Nanyang Technological University", "싱가포르"),
                new UniversitySearchResponse(12L, "National University of Singapore", "싱가포르")));

        mockMvc.perform(get(URL).param("keyword", "Nan").header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("S200"))
                .andExpect(jsonPath("$.message").value("조회되었습니다."))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].universityId").value(31))
                .andExpect(jsonPath("$.data[0].name").value("Nanyang Technological University"))
                .andExpect(jsonPath("$.data[0].country").value("싱가포르"))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    void returnsEmptyArrayWhenNoMatch() throws Exception {
        when(universityService.search("zzz")).thenReturn(List.of());

        mockMvc.perform(get(URL).param("keyword", "zzz").header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void missingKeywordIsValidatedByServiceWithFieldError() throws Exception {
        when(universityService.search(null)).thenThrow(new CustomException(ErrorCode.INVALID_INPUT,
                ErrorCode.INVALID_INPUT.getMessage(), List.of(new FieldErrorDetail("keyword", "검색어를 입력해 주세요"))));

        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E400"))
                .andExpect(jsonPath("$.message").value("입력값이 올바르지 않습니다"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.errors[0].field").value("keyword"));
        verify(universityService).search(null);
    }
}
