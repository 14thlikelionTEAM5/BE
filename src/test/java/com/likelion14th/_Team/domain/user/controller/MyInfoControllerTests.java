package com.likelion14th._Team.domain.user.controller;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.likelion14th._Team.domain.user.dto.MyInfoResponse;
import com.likelion14th._Team.domain.user.dto.MyInfoResponse.LanguageScoreItem;
import com.likelion14th._Team.domain.user.dto.MyInfoUpdateRequest;
import com.likelion14th._Team.domain.user.dto.MyInfoUpdateResponse;
import com.likelion14th._Team.domain.user.dto.PasswordChangeRequest;
import com.likelion14th._Team.domain.user.service.MyInfoService;
import com.likelion14th._Team.domain.user.service.PasswordService;
import com.likelion14th._Team.global.enums.LanguageTestType;
import com.likelion14th._Team.global.enums.MajorType;
import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import com.likelion14th._Team.global.security.JwtProvider;
import com.likelion14th._Team.global.security.SecurityConfig;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(controllers = MyInfoController.class,
        properties = "app.cors.allowed-origins=http://localhost:3000")
@Import(SecurityConfig.class)
class MyInfoControllerTests {

    private static final String URL = "/api/v1/users/me";
    private static final String PASSWORD_URL = "/api/v1/users/me/password";
    private static final String BEARER = "Bearer test-token";

    @Autowired
    MockMvc mvc;
    @MockitoBean
    MyInfoService myInfoService;
    @MockitoBean
    PasswordService passwordService;
    @MockitoBean
    JwtProvider jwt;

    @BeforeEach
    void setUp() {
        when(jwt.getUserId("test-token")).thenReturn(1L);
    }

    private ResultActions patchMe(String body) throws Exception {
        return mvc.perform(patch(URL).header("Authorization", BEARER)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions patchPassword(String body) throws Exception {
        return mvc.perform(patch(PASSWORD_URL).header("Authorization", BEARER)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    /** 어느 본문에서 실패했는지 알 수 있도록 본문을 메시지에 붙인다 */
    private void assertBadRequest(ResultActions actions, String body) {
        try {
            actions.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E400"));
        } catch (Exception | AssertionError e) {
            throw new AssertionError("400이 아님: " + body + " / " + e.getMessage(), e);
        }
    }

    // ===== GET /users/me =====

    @Test
    void getReturnsMyInfoWithConsentFlag() throws Exception {
        when(myInfoService.getMyInfo(1L)).thenReturn(new MyInfoResponse(
                1L, "chaewon@hufs.ac.kr", "채원", MajorType.PRIMARY, new BigDecimal("4.12"),
                List.of(new LanguageScoreItem(LanguageTestType.TOEFL_IBT, new BigDecimal("100.0"))), true));

        mvc.perform(get(URL).header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("S200"))
                .andExpect(jsonPath("$.message").value("조회되었습니다."))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.schoolEmail").value("chaewon@hufs.ac.kr"))
                .andExpect(jsonPath("$.data.nickname").value("채원"))
                .andExpect(jsonPath("$.data.majorType").value("PRIMARY"))
                .andExpect(jsonPath("$.data.gpa").value(4.12))
                .andExpect(jsonPath("$.data.languageScores[0].testType").value("TOEFL_IBT"))
                .andExpect(jsonPath("$.data.languageScores[0].score").value(100.0))
                .andExpect(jsonPath("$.data.externalAiConsented").value(true));
    }

    @Test
    void getWithoutTokenIs401() throws Exception {
        mvc.perform(get(URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("E401"));
        verifyNoInteractions(myInfoService);
    }

    // ===== PATCH /users/me =====

    @Test
    void emptyBodyPassesNullLanguageScoresToService() throws Exception {
        when(myInfoService.update(eq(1L), any())).thenReturn(new MyInfoUpdateResponse(
                1L, "chaewon@hufs.ac.kr", "채원", MajorType.PRIMARY, new BigDecimal("4.12"), List.of()));

        patchMe("{}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("수정되었습니다."))
                .andExpect(jsonPath("$.data.nickname").value("채원"))
                .andExpect(jsonPath("$.data.externalAiConsented").doesNotExist());

        ArgumentCaptor<MyInfoUpdateRequest> captor = ArgumentCaptor.forClass(MyInfoUpdateRequest.class);
        verify(myInfoService).update(eq(1L), captor.capture());
        assertNull(captor.getValue().languageScores());
        assertNull(captor.getValue().nickname());
        assertNull(captor.getValue().majorType());
        assertNull(captor.getValue().gpa());
    }

    @Test
    void emptyArrayIsDistinguishedFromOmitted() throws Exception {
        when(myInfoService.update(eq(1L), any())).thenReturn(new MyInfoUpdateResponse(
                1L, "chaewon@hufs.ac.kr", "채원", MajorType.PRIMARY, new BigDecimal("4.12"), List.of()));

        patchMe("{\"languageScores\":[]}").andExpect(status().isOk());

        ArgumentCaptor<MyInfoUpdateRequest> captor = ArgumentCaptor.forClass(MyInfoUpdateRequest.class);
        verify(myInfoService).update(eq(1L), captor.capture());
        assertNotNull(captor.getValue().languageScores());
        assertTrue(captor.getValue().languageScores().isEmpty());
    }

    @Test
    void validBodyIsPassedToService() throws Exception {
        when(myInfoService.update(eq(1L), any())).thenReturn(new MyInfoUpdateResponse(
                1L, "chaewon@hufs.ac.kr", "채원2", MajorType.DOUBLE, new BigDecimal("4.2"),
                List.of(new LanguageScoreItem(LanguageTestType.TOEFL_IBT, new BigDecimal("105.0")))));

        patchMe("""
                {"nickname":"채원2","majorType":"DOUBLE","gpa":4.2,
                 "languageScores":[{"testType":"TOEFL_IBT","score":105.0}]}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.majorType").value("DOUBLE"))
                .andExpect(jsonPath("$.data.languageScores[0].score").value(105.0));

        ArgumentCaptor<MyInfoUpdateRequest> captor = ArgumentCaptor.forClass(MyInfoUpdateRequest.class);
        verify(myInfoService).update(eq(1L), captor.capture());
        assertTrue(captor.getValue().nickname().equals("채원2"));
        assertTrue(captor.getValue().languageScores().size() == 1);
    }

    @Test
    void invalidBodiesAreRejectedBeforeReachingService() throws Exception {
        String[] bodies = {
                "{\"nickname\":\"a\"}",
                "{\"nickname\":\"abcdefghijklm\"}",
                "{\"nickname\":\"채원!!\"}",
                "{\"nickname\":\"\"}",
                "{\"gpa\":4.6}",
                "{\"gpa\":-0.1}",
                "{\"gpa\":3.456}",
                "{\"majorType\":\"MAJOR\"}",
                "{\"languageScores\":[{\"testType\":\"IELTS\",\"score\":7.3}]}",
                "{\"languageScores\":[{\"testType\":\"IELTS\",\"score\":9.5}]}",
                "{\"languageScores\":[{\"testType\":\"TOEFL_IBT\",\"score\":100.5}]}",
                "{\"languageScores\":[{\"testType\":\"TOEFL_IBT\",\"score\":121}]}",
                "{\"languageScores\":[{\"testType\":\"TOEFL_ITP\",\"score\":300}]}",
                "{\"languageScores\":[{\"testType\":\"TOEFL_NEW\",\"score\":6.5}]}",
                "{\"languageScores\":[{\"testType\":\"TOEFL_IBT\",\"score\":100},{\"testType\":\"TOEFL_IBT\",\"score\":90}]}",
                "{\"languageScores\":[null]}",
                "{\"languageScores\":[{\"score\":100}]}",
                "{\"languageScores\":[{\"testType\":\"IELTS\"}]}"
        };
        for (String body : bodies) {
            assertBadRequest(patchMe(body), body);
        }
        verifyNoInteractions(myInfoService);
    }

    @Test
    void validationErrorsNameTheField() throws Exception {
        patchMe("{\"nickname\":\"a\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("nickname"));
        patchMe("{\"gpa\":4.6}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("gpa"));
    }

    @Test
    void duplicateNicknameFromServiceIs409() throws Exception {
        when(myInfoService.update(eq(1L), any()))
                .thenThrow(new CustomException(ErrorCode.NICKNAME_DUPLICATED));

        patchMe("{\"nickname\":\"타인\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("E409"))
                .andExpect(jsonPath("$.message").value("이미 사용 중인 닉네임입니다"));
    }

    @Test
    void patchWithoutTokenIs401() throws Exception {
        mvc.perform(patch(URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("E401"));
        verifyNoInteractions(myInfoService);
    }

    // ===== PATCH /users/me/password =====

    @Test
    void changePasswordReturnsMessageWithNullData() throws Exception {
        patchPassword("{\"currentPassword\":\"Passhufs1234!\",\"newPassword\":\"Newpass5678!\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("S200"))
                .andExpect(jsonPath("$.message").value("비밀번호가 변경되었습니다."))
                .andExpect(jsonPath("$.data").isEmpty());

        verify(passwordService).changePassword(1L, new PasswordChangeRequest("Passhufs1234!", "Newpass5678!"));
    }

    @Test
    void invalidPasswordBodiesAreRejectedBeforeReachingService() throws Exception {
        String[] bodies = {
                "{\"currentPassword\":\"\",\"newPassword\":\"Newpass5678!\"}",
                "{\"currentPassword\":\"Passhufs1234!\",\"newPassword\":\"short1!\"}",
                "{\"currentPassword\":\"Passhufs1234!\",\"newPassword\":\"\"}",
                "{\"currentPassword\":\"Passhufs1234!\"}",
                "{\"newPassword\":\"Newpass5678!\"}",
                "{\"currentPassword\":\"Passhufs1234!\",\"newPassword\":\"" + "a".repeat(65) + "\"}"
        };
        for (String body : bodies) {
            assertBadRequest(patchPassword(body), body);
        }
        verifyNoInteractions(passwordService);
    }

    @Test
    void wrongCurrentPasswordIs400WithoutFieldErrors() throws Exception {
        doThrow(new CustomException(ErrorCode.CURRENT_PASSWORD_MISMATCH))
                .when(passwordService).changePassword(eq(1L), any());

        patchPassword("{\"currentPassword\":\"wrong-password\",\"newPassword\":\"Newpass5678!\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E400"))
                .andExpect(jsonPath("$.message").value("현재 비밀번호가 올바르지 않습니다"))
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    void changePasswordWithoutTokenIs401() throws Exception {
        mvc.perform(patch(PASSWORD_URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"Passhufs1234!\",\"newPassword\":\"Newpass5678!\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("E401"));
        verifyNoInteractions(passwordService);
    }
}