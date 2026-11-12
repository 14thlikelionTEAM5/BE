package com.likelion14th._Team.domain.agreement;

import com.likelion14th._Team.domain.agreement.controller.AgreementChangesController;
import com.likelion14th._Team.domain.agreement.dto.AgreementChangesResponse;
import com.likelion14th._Team.domain.agreement.service.AgreementChangesService;
import com.likelion14th._Team.global.security.*;
import com.likelion14th._Team.global.exception.*;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.*;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDate;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers=AgreementChangesController.class,properties="app.cors.allowed-origins=http://localhost:3000")
@Import(SecurityConfig.class)
@ExtendWith(OutputCaptureExtension.class)
class AgreementChangesControllerTests {
    @Autowired MockMvc mvc;
    @MockitoBean AgreementChangesService service;
    @MockitoBean JwtProvider jwt;
    static final String URL="/api/v1/agreements/changes";
    @Test void missingInvalidExpiredTokensAre401() throws Exception {
        mvc.perform(get(URL)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("E401"))
            .andExpect(jsonPath("$.message").value("로그인이 필요합니다"));
        for(String token:new String[]{"invalid","expired"}) {
            when(jwt.getUserId(token)).thenThrow(new JwtException(token));
            mvc.perform(get(URL).header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());
        }
        verifyNoInteractions(service);
    }
    @Test void successMatchesSpecification() throws Exception {
        when(jwt.getUserId("valid")).thenReturn(1L);
        when(service.changes()).thenReturn(new AgreementChangesResponse("2027-1","2026-2",LocalDate.parse("2026-07-03"),3L,5L,2L));
        mvc.perform(get(URL).header("Authorization","Bearer valid")).andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("S200")).andExpect(jsonPath("$.message").value("이번 학기 변경 안내 조회에 성공했습니다."))
            .andExpect(jsonPath("$.data.newCount").value(3)).andExpect(jsonPath("$.data.quotaChangedCount").value(5))
            .andExpect(jsonPath("$.data.closedCount").value(2)).andExpect(jsonPath("$.data.standardDate").value("2026-07-03"))
            .andExpect(jsonPath("$.errors").doesNotExist());
    }
    @Test void noAgreementIs404() throws Exception {
        when(jwt.getUserId("valid")).thenReturn(1L);
        when(service.changes()).thenThrow(new CustomException(ErrorCode.AGREEMENT_NOT_FOUND));
        mvc.perform(get(URL).header("Authorization","Bearer valid")).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("E404")).andExpect(jsonPath("$.message").value("협정교 정보가 없습니다"))
            .andExpect(jsonPath("$.data").doesNotExist());
    }
    @Test void failureIs500WithoutSecretLogging(CapturedOutput output) throws Exception {
        String marker=UUID.randomUUID().toString();when(jwt.getUserId(marker)).thenReturn(1L);
        when(service.changes()).thenThrow(new IllegalStateException(marker,new RuntimeException(marker)));
        mvc.perform(get(URL).header("Authorization","Bearer "+marker).param("password",marker).param("authCode",marker))
            .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value("E500"))
            .andExpect(jsonPath("$.message").value("변경 안내 조회 중 오류가 발생했습니다"));
        assertFalse(output.getAll().contains(marker));
    }
}
