package com.likelion14th._Team.domain.university;

import com.likelion14th._Team.domain.university.controller.UniversityReturnReportController;
import com.likelion14th._Team.domain.university.service.UniversityReturnReportService;
import com.likelion14th._Team.domain.university.dto.UniversityReturnReportsResponse;
import com.likelion14th._Team.domain.university.dto.UniversityReturnReportsResponse.Report;
import com.likelion14th._Team.global.security.*;
import com.likelion14th._Team.global.exception.*;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.*;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers=UniversityReturnReportController.class,properties="app.cors.allowed-origins=http://localhost:3000")
@Import({SecurityConfig.class, ActiveUserTestConfig.class})
@ExtendWith(OutputCaptureExtension.class)
class UniversityReturnReportControllerTests {
    @Autowired MockMvc mvc;
    @MockitoBean UniversityReturnReportService service;
    @MockitoBean JwtProvider jwt;
    String url(String id) {return "/api/v1/universities/"+id+"/return-reports";}
    @BeforeEach void setup(){when(jwt.getUserId("valid")).thenReturn(1L);}
    @Test void noOrInvalidOrExpiredTokenIs401BeforeValidation() throws Exception {
        mvc.perform(get(url("abc"))).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("E401"));
        for(String token:List.of("invalid","expired")) {
            when(jwt.getUserId(token)).thenThrow(new JwtException(token));
            mvc.perform(get(url("1")).header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());
        }
        verifyNoInteractions(service);
    }
    @Test void invalidIdsHave400FieldErrors() throws Exception {
        for(String id:List.of("0","-1","1.5","abc","9223372036854775808")) {
            mvc.perform(get(url(id)).header("Authorization","Bearer valid"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E400"))
                .andExpect(jsonPath("$.errors[0].field").value("universityId"));
        }
        verifyNoInteractions(service);
    }
    @Test void successKeepsRawStringsAndNullAndOmitsAuthor() throws Exception {
        var report=new Report(2L,"2026-1","왕복 $1,200",null,"월 150~200만원",null,"DORM",null,null,null,"저장된 요약");
        when(service.reports(4L)).thenReturn(new UniversityReturnReportsResponse(4L,1,List.of(report)));
        mvc.perform(get(url("4")).header("Authorization","Bearer valid"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.message").value("귀국보고서 조회에 성공했습니다."))
            .andExpect(jsonPath("$.data.totalCount").value(1))
            .andExpect(jsonPath("$.data.returnReports[0].costFlight").value("왕복 $1,200"))
            .andExpect(jsonPath("$.data.returnReports[0].costInsurance").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$.data.returnReports[0].reviewSummary").value("저장된 요약"))
            .andExpect(jsonPath("$.data.returnReports[0].userId").doesNotExist())
            .andExpect(jsonPath("$.data.returnReports[0].nickname").doesNotExist())
            .andExpect(jsonPath("$.data.returnReports[0].schoolEmail").doesNotExist());
    }
    @Test void emptyListIs200() throws Exception {
        when(service.reports(4L)).thenReturn(new UniversityReturnReportsResponse(4L,0,List.of()));
        mvc.perform(get(url("4")).header("Authorization","Bearer valid"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalCount").value(0))
            .andExpect(jsonPath("$.data.returnReports").isArray()).andExpect(jsonPath("$.data.returnReports").isEmpty());
    }
    @Test void missingSchoolIs404() throws Exception {
        when(service.reports(4L)).thenThrow(new CustomException(ErrorCode.UNIVERSITY_NOT_FOUND,"존재하지 않는 학교입니다"));
        mvc.perform(get(url("4")).header("Authorization","Bearer valid"))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("E404"))
            .andExpect(jsonPath("$.message").value("존재하지 않는 학교입니다"));
    }
    @Test void failureDoesNotLogSecrets(CapturedOutput output) throws Exception {
        String marker=UUID.randomUUID().toString();when(jwt.getUserId(marker)).thenReturn(1L);
        when(service.reports(4L)).thenThrow(new IllegalStateException(marker,new RuntimeException(marker)));
        mvc.perform(get(url("4")).header("Authorization","Bearer "+marker).param("password",marker).param("authCode",marker))
            .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value("E500"))
            .andExpect(jsonPath("$.message").value("귀국보고서 조회 중 오류가 발생했습니다"));
        assertFalse(output.getAll().contains(marker));
    }
}
