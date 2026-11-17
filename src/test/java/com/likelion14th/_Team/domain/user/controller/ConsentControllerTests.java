package com.likelion14th._Team.domain.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.likelion14th._Team.domain.user.dto.ConsentRequest;
import com.likelion14th._Team.domain.user.dto.ConsentResponse;
import com.likelion14th._Team.domain.user.dto.ConsentResult;
import com.likelion14th._Team.domain.user.service.ConsentService;
import com.likelion14th._Team.global.enums.ConsentType;
import com.likelion14th._Team.global.security.JwtProvider;
import com.likelion14th._Team.global.security.SecurityConfig;
import com.likelion14th._Team.global.security.ActiveUserTestConfig;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ConsentController.class, properties = "app.cors.allowed-origins=http://localhost:3000")
@Import({SecurityConfig.class, ActiveUserTestConfig.class})
class ConsentControllerTests {

    private static final String URL = "/api/v1/consents";
    private static final String BEARER = "Bearer valid-token";
    private static final String BODY = "{\"consentType\":\"EXTERNAL_AI\"}";
    private static final ConsentResponse CONSENT =
            new ConsentResponse(ConsentType.EXTERNAL_AI, LocalDateTime.of(2026, 10, 30, 19, 2, 0));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConsentService consentService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        when(jwtProvider.getUserId("valid-token")).thenReturn(1L);
    }

    @Test
    void returns401InCommonFormatWithoutToken() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("E401"))
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
        verifyNoInteractions(consentService);
    }

    @Test
    void firstConsentReturns201() throws Exception {
        when(consentService.record(eq(1L), any(ConsentRequest.class))).thenReturn(new ConsentResult(CONSENT, true));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY)
                        .header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("S201"))
                .andExpect(jsonPath("$.message").value("동의가 기록되었습니다."))
                .andExpect(jsonPath("$.data.consentType").value("EXTERNAL_AI"))
                .andExpect(jsonPath("$.data.consentedAt").value("2026-10-30T19:02:00"))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    void existingConsentReturns200() throws Exception {
        when(consentService.record(eq(1L), any(ConsentRequest.class))).thenReturn(new ConsentResult(CONSENT, false));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY)
                        .header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("S200"))
                .andExpect(jsonPath("$.message").value("이미 동의한 기록입니다."))
                .andExpect(jsonPath("$.data.consentType").value("EXTERNAL_AI"))
                .andExpect(jsonPath("$.data.consentedAt").value("2026-10-30T19:02:00"));
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"consentType\":")
                        .header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E400"))
                .andExpect(jsonPath("$.message").value("입력값이 올바르지 않습니다"));
        verifyNoInteractions(consentService);
    }
}
