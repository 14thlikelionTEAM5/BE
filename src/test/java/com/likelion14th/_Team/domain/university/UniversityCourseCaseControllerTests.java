package com.likelion14th._Team.domain.university;

import com.likelion14th._Team.domain.university.controller.UniversityCourseCaseController;
import com.likelion14th._Team.domain.university.service.UniversityCourseCaseService;
import com.likelion14th._Team.domain.university.dto.UniversityCourseCasesResponse;
import com.likelion14th._Team.domain.university.dto.UniversityCourseCasesResponse.Item;
import com.likelion14th._Team.global.security.*;
import com.likelion14th._Team.global.enums.*;
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
import java.math.BigDecimal;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers=UniversityCourseCaseController.class,properties="app.cors.allowed-origins=http://localhost:3000")
@Import(SecurityConfig.class)
@ExtendWith(OutputCaptureExtension.class)
class UniversityCourseCaseControllerTests {
    @Autowired MockMvc mvc;
    @MockitoBean UniversityCourseCaseService service;
    @MockitoBean JwtProvider jwt;
    String url(String id) {return "/api/v1/universities/"+id+"/course-cases";}
    @BeforeEach void setup(){when(jwt.getUserId("valid")).thenReturn(1L);}
    @Test void absentInvalidExpiredTokenIs401() throws Exception {
        mvc.perform(get(url("abc"))).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("E401"));
        for(String token:List.of("invalid","expired")) {
            when(jwt.getUserId(token)).thenThrow(new JwtException(token));
            mvc.perform(get(url("1")).header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());
        }
        verifyNoInteractions(service);
    }
    @Test void invalidIdHasFieldError() throws Exception {
        for(String id:List.of("0","-1","1.5","abc","9223372036854775808")) {
            mvc.perform(get(url(id)).header("Authorization","Bearer valid"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E400"))
                .andExpect(jsonPath("$.errors[0].field").value("universityId"));
        }
        verifyNoInteractions(service);
    }
    @Test void successHasOnlyPublicFieldsAndGeneralNull() throws Exception {
        var item=new Item(7L,"International Marketing",null,CourseCategory.GENERAL,new BigDecimal("3.0"),MajorType.MINOR,"2026-1");
        when(service.cases(4L)).thenReturn(new UniversityCourseCasesResponse(4L,List.of(item)));
        mvc.perform(get(url("4")).header("Authorization","Bearer valid"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.message").value("인정 사례 목록 조회에 성공했습니다."))
            .andExpect(jsonPath("$.data.courseCases[0].courseCaseId").value(7))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"recognizedCredit\":3.0")))
            .andExpect(jsonPath("$.data.courseCases[0].recognitionType").value("GENERAL"))
            .andExpect(jsonPath("$.data.courseCases[0].majorType").value("MINOR"))
            .andExpect(jsonPath("$.data.courseCases[0].homeCourseName").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$.data.courseCases[0].userId").doesNotExist())
            .andExpect(jsonPath("$.data.courseCases[0].nickname").doesNotExist())
            .andExpect(jsonPath("$.data.courseCases[0].email").doesNotExist());
    }
    @Test void noCasesIsEmptyArray() throws Exception {
        when(service.cases(4L)).thenReturn(new UniversityCourseCasesResponse(4L,List.of()));
        mvc.perform(get(url("4")).header("Authorization","Bearer valid"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.courseCases").isArray())
            .andExpect(jsonPath("$.data.courseCases").isEmpty());
    }
    @Test void schoolNotFoundIs404() throws Exception {
        when(service.cases(4L)).thenThrow(new CustomException(ErrorCode.UNIVERSITY_NOT_FOUND,"존재하지 않는 학교입니다"));
        mvc.perform(get(url("4")).header("Authorization","Bearer valid"))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("존재하지 않는 학교입니다"));
    }
    @Test void failureDoesNotLogSecrets(CapturedOutput output) throws Exception {
        String marker=UUID.randomUUID().toString();when(jwt.getUserId(marker)).thenReturn(1L);
        when(service.cases(4L)).thenThrow(new IllegalStateException(marker,new RuntimeException(marker)));
        mvc.perform(get(url("4")).header("Authorization","Bearer "+marker).param("password",marker).param("authCode",marker))
            .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value("E500"))
            .andExpect(jsonPath("$.message").value("인정 사례 목록 조회 중 오류가 발생했습니다"));
        assertFalse(output.getAll().contains(marker));
    }
}
