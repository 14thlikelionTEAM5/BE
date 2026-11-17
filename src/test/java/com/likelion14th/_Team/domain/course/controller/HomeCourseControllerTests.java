package com.likelion14th._Team.domain.course.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.likelion14th._Team.domain.course.dto.HomeCourseSearchResponse;
import com.likelion14th._Team.domain.course.service.HomeCourseService;
import com.likelion14th._Team.global.enums.CourseCategory;
import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import com.likelion14th._Team.global.response.FieldErrorDetail;
import com.likelion14th._Team.global.security.JwtProvider;
import com.likelion14th._Team.global.security.SecurityConfig;
import com.likelion14th._Team.global.security.ActiveUserTestConfig;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = HomeCourseController.class, properties = "app.cors.allowed-origins=http://localhost:3000")
@Import({SecurityConfig.class, ActiveUserTestConfig.class})
class HomeCourseControllerTests {

    private static final String URL = "/api/v1/home-courses";
    private static final String BEARER = "Bearer valid-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HomeCourseService homeCourseService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        when(jwtProvider.getUserId("valid-token")).thenReturn(1L);
    }

    @Test
    void returns401InCommonFormatWithoutToken() throws Exception {
        mockMvc.perform(get(URL).param("keyword", "마케팅").param("recognitionType", "MAJOR_ELECTIVE"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("E401"))
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
        verifyNoInteractions(homeCourseService);
    }

    @Test
    void searchReturnsCoursesWithOneDecimalCredit() throws Exception {
        when(homeCourseService.search("마케팅", "MAJOR_ELECTIVE")).thenReturn(List.of(
                new HomeCourseSearchResponse(305L, "BUS2101", "마케팅원론", new BigDecimal("3.0"),
                        CourseCategory.MAJOR_ELECTIVE)));

        mockMvc.perform(get(URL).param("keyword", "마케팅").param("recognitionType", "MAJOR_ELECTIVE")
                        .header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("S200"))
                .andExpect(jsonPath("$.message").value("조회되었습니다."))
                .andExpect(jsonPath("$.data[0].homeCourseId").value(305))
                .andExpect(jsonPath("$.data[0].code").value("BUS2101"))
                .andExpect(jsonPath("$.data[0].name").value("마케팅원론"))
                .andExpect(jsonPath("$.data[0].category").value("MAJOR_ELECTIVE"))
                .andExpect(content().string(containsString("\"credit\":3.0")));
    }

    @Test
    void invalidRecognitionTypeReturns400WithFieldError() throws Exception {
        when(homeCourseService.search("마케팅", "GENERAL")).thenThrow(new CustomException(ErrorCode.INVALID_INPUT,
                ErrorCode.INVALID_INPUT.getMessage(),
                List.of(new FieldErrorDetail("recognitionType", "MAJOR_REQUIRED 또는 MAJOR_ELECTIVE만 사용할 수 있습니다"))));

        mockMvc.perform(get(URL).param("keyword", "마케팅").param("recognitionType", "GENERAL")
                        .header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E400"))
                .andExpect(jsonPath("$.message").value("입력값이 올바르지 않습니다"))
                .andExpect(jsonPath("$.errors[0].field").value("recognitionType"));
    }

    @Test
    void serverErrorReturns500WithApiMessage() throws Exception {
        when(homeCourseService.search("마케팅", "MAJOR_REQUIRED"))
                .thenThrow(new CustomException(ErrorCode.INTERNAL_ERROR, "본교 과목 검색 중 오류가 발생했습니다."));

        mockMvc.perform(get(URL).param("keyword", "마케팅").param("recognitionType", "MAJOR_REQUIRED")
                        .header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("E500"))
                .andExpect(jsonPath("$.message").value("본교 과목 검색 중 오류가 발생했습니다."));
    }
}
