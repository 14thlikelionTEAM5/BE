package com.likelion14th._Team.domain.returnreport.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.likelion14th._Team.domain.returnreport.dto.ReturnReportCost;
import com.likelion14th._Team.domain.returnreport.dto.ReturnReportExtractRequest;
import com.likelion14th._Team.domain.returnreport.dto.ReturnReportExtractResponse;
import com.likelion14th._Team.domain.returnreport.dto.ReturnReportSaveRequest;
import com.likelion14th._Team.domain.returnreport.dto.ReturnReportSaveResponse;
import com.likelion14th._Team.domain.returnreport.dto.ReturnReportUpdateRequest;
import com.likelion14th._Team.domain.returnreport.dto.ReturnReportVisa;
import com.likelion14th._Team.domain.returnreport.service.ReturnReportExtractService;
import com.likelion14th._Team.domain.returnreport.service.ReturnReportService;
import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import com.likelion14th._Team.global.response.FieldErrorDetail;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import com.likelion14th._Team.domain.user.service.ConsentService;

@WebMvcTest(controllers = ReturnReportController.class, properties = "app.cors.allowed-origins=http://localhost:3000")
@Import(SecurityConfig.class)
class ReturnReportControllerTests {

    private static final String URL = "/api/v1/return-reports";
    private static final String BEARER = "Bearer valid-token";
    private static final String SAVE_BODY = """
            {"universityId": 12, "semester": "2026-2",
             "cost": {"flight": "왕복 약 90만원", "insurance": "약 30만원", "livingMonthly": "월 150만원", "total": "약 1,200만원"},
             "housingType": "DORM", "visa": {"duration": "약 2주", "type": "Student Pass"},
             "insuranceProvider": "삼성화재",
             "courses": [{"courseCode": "MKT2101", "courseNameEn": "Principles of Marketing", "foreignCredit": 4.0, "satisfaction": 4}],
             "reviewSummary": "기숙사 생활이 편리했다."}
            """;
    private static final ReturnReportSaveResponse SAVED = new ReturnReportSaveResponse(5L, 12L, "2026-2",
            List.of(new ReturnReportSaveResponse.CourseSummary(21L, "MKT2101", "Principles of Marketing")));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReturnReportService returnReportService;

    @MockitoBean
    private ReturnReportExtractService returnReportExtractService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private ConsentService consentService;

    @BeforeEach
    void setUp() {
        when(jwtProvider.getUserId("valid-token")).thenReturn(1L);
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String body) {
        return builder.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder builder) {
        return builder.header(HttpHeaders.AUTHORIZATION, BEARER);
    }

    @Test
    void allEndpointsReturn401InCommonFormatWithoutToken() throws Exception {
        MockHttpServletRequestBuilder[] requests = {
                json(post(URL + "/extract"), "{\"text\":\"보고서\"}"),
                json(post(URL), SAVE_BODY),
                json(put(URL + "/5"), "{\"courses\":[]}"),
                delete(URL + "/5")};
        for (MockHttpServletRequestBuilder request : requests) {
            mockMvc.perform(request)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("E401"))
                    .andExpect(jsonPath("$.message").value("로그인이 필요합니다."))
                    .andExpect(jsonPath("$.data").doesNotExist())
                    .andExpect(jsonPath("$.errors").doesNotExist())
                    .andExpect(jsonPath("$.timestamp").isNotEmpty());
        }
        verifyNoInteractions(returnReportService, returnReportExtractService, consentService);
    }

    // ===== 추출 =====

    @Test
    void extractReturnsApiFields() throws Exception {
        when(returnReportExtractService.extract(any())).thenReturn(new ReturnReportExtractResponse(
                new ReturnReportCost("왕복 약 90만원", null, null, null), "DORM", ReturnReportVisa.EMPTY, null,
                List.of(new ReturnReportExtractResponse.ExtractedCourse("MKT2101", "Principles of Marketing",
                        new BigDecimal("4.0"), 4)), null));

        mockMvc.perform(authed(json(post(URL + "/extract"), "{\"text\":\"보고서 내용\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("S200"))
                .andExpect(jsonPath("$.message").value("항목이 추출되었습니다."))
                .andExpect(jsonPath("$.data.cost.flight").value("왕복 약 90만원"))
                .andExpect(jsonPath("$.data.cost.insurance").value((Object) null))
                .andExpect(jsonPath("$.data.housingType").value("DORM"))
                .andExpect(jsonPath("$.data.visa.duration").value((Object) null))
                .andExpect(jsonPath("$.data.insuranceProvider").value((Object) null))
                .andExpect(jsonPath("$.data.courses[0].courseCode").value("MKT2101"))
                .andExpect(jsonPath("$.data.courses[0].courseNameEn").value("Principles of Marketing"))
                .andExpect(jsonPath("$.data.courses[0].satisfaction").value(4))
                .andExpect(jsonPath("$.data.reviewSummary").value((Object) null))
                .andExpect(content().string(containsString("\"foreignCredit\":4.0")));
    }

    @Test
    void extractRejectsBlankOrTooLongTextWithoutEchoingInput() throws Exception {
        String tooLong = "개인정보" + "가".repeat(20000);
        for (String text : new String[]{"   ", tooLong}) {
            mockMvc.perform(authed(json(post(URL + "/extract"), "{\"text\":\"" + text + "\"}")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("E400"))
                    .andExpect(jsonPath("$.message").value("입력값이 올바르지 않습니다"))
                    .andExpect(jsonPath("$.errors[0].field").value("text"))
                    .andExpect(content().string(not(containsString("개인정보"))));
        }
        mockMvc.perform(authed(json(post(URL + "/extract"), "{\"text\":\"" + "가".repeat(20000) + "\"}")))
                .andExpect(status().isOk());
    }

    @Test
    void extractFailureIsSingleE500() throws Exception {
        when(returnReportExtractService.extract(any())).thenThrow(new CustomException(ErrorCode.INTERNAL_ERROR,
                "항목 추출에 실패했습니다. 다시 시도하거나 직접 입력해 주세요."));

        mockMvc.perform(authed(json(post(URL + "/extract"), "{\"text\":\"보고서\"}")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("E500"))
                .andExpect(jsonPath("$.message").value("항목 추출에 실패했습니다. 다시 시도하거나 직접 입력해 주세요."));
    }

    @Test
    void extractWithoutExternalAiConsentReturns403() throws Exception {
        doThrow(new CustomException(ErrorCode.EXTERNAL_AI_CONSENT_REQUIRED))
                .when(consentService).requireExternalAiConsent(1L);

        mockMvc.perform(authed(json(post(URL + "/extract"), "{\"text\":\"보고서\"}")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("E403"))
                .andExpect(jsonPath("$.message").value("외부 AI 처리 동의가 필요합니다"))
                .andExpect(jsonPath("$.data").doesNotExist());
        verifyNoInteractions(returnReportExtractService);
    }

    @Test
    void extractChecksConsentOfLoggedInUser() throws Exception {
        when(returnReportExtractService.extract(any())).thenReturn(new ReturnReportExtractResponse(
                ReturnReportCost.EMPTY, null, ReturnReportVisa.EMPTY, null, List.of(), null));

        mockMvc.perform(authed(json(post(URL + "/extract"), "{\"text\":\"보고서\"}")))
                .andExpect(status().isOk());

        verify(consentService).requireExternalAiConsent(1L);
    }
    @Test
    void extractValidatesInputBeforeCheckingConsent() throws Exception {
        mockMvc.perform(authed(json(post(URL + "/extract"), "{\"text\":\"   \"}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(consentService, returnReportExtractService);
    }

    // ===== 저장 =====

    @Test
    void saveReturns201() throws Exception {
        when(returnReportService.save(eq(1L), any())).thenReturn(SAVED);

        mockMvc.perform(authed(json(post(URL), SAVE_BODY)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("S201"))
                .andExpect(jsonPath("$.message").value("귀국보고서가 저장되었습니다."))
                .andExpect(jsonPath("$.data.returnReportId").value(5))
                .andExpect(jsonPath("$.data.universityId").value(12))
                .andExpect(jsonPath("$.data.semester").value("2026-2"))
                .andExpect(jsonPath("$.data.courses[0].courseId").value(21))
                .andExpect(jsonPath("$.data.courses[0].courseCode").value("MKT2101"))
                .andExpect(jsonPath("$.data.courses[0].courseNameEn").value("Principles of Marketing"));
    }

    @Test
    void saveTrimsStringsAndTreatsBlankAsNull() throws Exception {
        when(returnReportService.save(eq(1L), any())).thenReturn(SAVED);
        String body = """
                {"universityId": 12, "semester": " 2026-2 ", "cost": {"flight": "  ", "total": " 약 1,200만원 "},
                 "housingType": " DORM ", "insuranceProvider": "", "visa": null,
                 "courses": [{"courseCode": " ", "courseNameEn": " Marketing ", "foreignCredit": 3}]}
                """;

        mockMvc.perform(authed(json(post(URL), body))).andExpect(status().isCreated());

        ArgumentCaptor<ReturnReportSaveRequest> captor = ArgumentCaptor.forClass(ReturnReportSaveRequest.class);
        verify(returnReportService).save(eq(1L), captor.capture());
        ReturnReportSaveRequest request = captor.getValue();
        assertEqualsTrimmed(request);
    }

    private static void assertEqualsTrimmed(ReturnReportSaveRequest request) {
        org.junit.jupiter.api.Assertions.assertEquals("2026-2", request.semester());
        org.junit.jupiter.api.Assertions.assertNull(request.cost().flight());
        org.junit.jupiter.api.Assertions.assertEquals("약 1,200만원", request.cost().total());
        org.junit.jupiter.api.Assertions.assertEquals("DORM", request.housingType());
        org.junit.jupiter.api.Assertions.assertNull(request.insuranceProvider());
        org.junit.jupiter.api.Assertions.assertNull(request.visa());
        org.junit.jupiter.api.Assertions.assertNull(request.courses().getFirst().courseCode());
        org.junit.jupiter.api.Assertions.assertEquals("Marketing", request.courses().getFirst().courseNameEn());
    }

    @Test
    void saveValidationReportsFieldErrors() throws Exception {
        String body = """
                {"semester": "2026-3", "housingType": "dorm", "cost": {"flight": "%s"},
                 "visa": {"type": "%s"}, "insuranceProvider": "%s",
                 "courses": [
                   {"courseNameEn": " ", "foreignCredit": 0, "satisfaction": 6},
                   {"courseCode": "%s", "courseNameEn": "A", "foreignCredit": 100, "satisfaction": 0},
                   {"courseNameEn": "B", "foreignCredit": 4.25},
                   null
                 ]}
                """.formatted("가".repeat(201), "가".repeat(101), "가".repeat(201), "C".repeat(51));

        mockMvc.perform(authed(json(post(URL), body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E400"))
                .andExpect(jsonPath("$.message").value("입력값이 올바르지 않습니다"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder(
                        "universityId", "semester", "housingType", "cost.flight", "visa.type", "insuranceProvider",
                        "courses[0].courseNameEn", "courses[0].foreignCredit", "courses[0].satisfaction",
                        // 100은 최댓값(99.9)과 자릿수(정수 2자리) 둘 다 위반이라 사유가 2개
                        "courses[1].courseCode", "courses[1].foreignCredit", "courses[1].foreignCredit",
                        "courses[1].satisfaction",
                        "courses[2].foreignCredit", "courses[3]")));
        verifyNoInteractions(returnReportService);
    }

    @Test
    void saveRequiresCoursesArrayButAllowsEmpty() throws Exception {
        String withoutCourses = "{\"universityId\": 12, \"semester\": \"2026-2\"}";
        mockMvc.perform(authed(json(post(URL), withoutCourses)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("courses"));

        when(returnReportService.save(eq(1L), any())).thenReturn(SAVED);
        mockMvc.perform(authed(json(post(URL), "{\"universityId\": 12, \"semester\": \"2026-2\", \"courses\": []}")))
                .andExpect(status().isCreated());
    }

    @Test
    void satisfactionMustBeJsonInteger() throws Exception {
        for (String value : new String[]{"4.5", "4.0"}) {
            String body = """
                    {"universityId": 12, "semester": "2026-2",
                     "courses": [{"courseNameEn": "A", "foreignCredit": 3, "satisfaction": %s}]}
                    """.formatted(value);
            mockMvc.perform(authed(json(post(URL), body)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("courses[0].satisfaction"))
                    .andExpect(jsonPath("$.errors[0].reason").value("수업 만족도는 정수여야 합니다"));
        }
        verifyNoInteractions(returnReportService);

        when(returnReportService.save(eq(1L), any())).thenReturn(SAVED);
        mockMvc.perform(authed(json(post(URL), """
                        {"universityId": 12, "semester": "2026-2",
                         "courses": [{"courseNameEn": "A", "foreignCredit": 3, "satisfaction": 4}]}
                        """)))
                .andExpect(status().isCreated());
        ArgumentCaptor<ReturnReportSaveRequest> captor = ArgumentCaptor.forClass(ReturnReportSaveRequest.class);
        verify(returnReportService).save(eq(1L), captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(4, captor.getValue().courses().getFirst().satisfactionValue());
    }

    @Test
    void saveErrorsKeepCommonFormat() throws Exception {
        Object[][] cases = {
                {new CustomException(ErrorCode.UNIVERSITY_NOT_FOUND, "존재하지 않는 학교입니다"), 404, "E404"},
                {new CustomException(ErrorCode.RETURN_REPORT_DUPLICATED), 409, "E409"}};
        for (Object[] c : cases) {
            CustomException exception = (CustomException) c[0];
            when(returnReportService.save(eq(1L), any())).thenThrow(exception);

            mockMvc.perform(authed(json(post(URL), SAVE_BODY)))
                    .andExpect(status().is((int) c[1]))
                    .andExpect(jsonPath("$.code").value(c[2]))
                    .andExpect(jsonPath("$.message").value(exception.getMessage()))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }
    }

    // ===== 수정 =====

    @Test
    void updateIgnoresUniversityAndSemesterInBody() throws Exception {
        when(returnReportService.update(eq(1L), eq(5L), any())).thenReturn(SAVED);
        String body = """
                {"universityId": 99, "semester": "1999-1", "housingType": "SELF",
                 "courses": [{"courseId": 21, "courseNameEn": "Principles of Marketing", "foreignCredit": 4.0}]}
                """;

        mockMvc.perform(authed(json(put(URL + "/5"), body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("S200"))
                .andExpect(jsonPath("$.message").value("귀국보고서가 수정되었습니다."))
                .andExpect(jsonPath("$.data.returnReportId").value(5));

        ArgumentCaptor<ReturnReportUpdateRequest> captor = ArgumentCaptor.forClass(ReturnReportUpdateRequest.class);
        verify(returnReportService).update(eq(1L), eq(5L), captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(21L, captor.getValue().courses().getFirst().courseId());
    }

    @Test
    void updateCourseIdErrorHasFieldReason() throws Exception {
        when(returnReportService.update(eq(1L), eq(5L), any())).thenThrow(new CustomException(ErrorCode.INVALID_INPUT,
                "수정할 수 없는 교과목이 포함되어 있습니다",
                List.of(new FieldErrorDetail("courses[0].courseId", "이 귀국보고서의 교과목이 아닙니다"))));

        mockMvc.perform(authed(json(put(URL + "/5"),
                        "{\"courses\":[{\"courseId\":999,\"courseNameEn\":\"A\",\"foreignCredit\":3}]}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E400"))
                .andExpect(jsonPath("$.message").value("수정할 수 없는 교과목이 포함되어 있습니다"))
                .andExpect(jsonPath("$.errors[0].field").value("courses[0].courseId"))
                .andExpect(jsonPath("$.errors[0].reason").value("이 귀국보고서의 교과목이 아닙니다"));
    }

    @Test
    void updateForbiddenAndNotFound() throws Exception {
        when(returnReportService.update(eq(1L), eq(5L), any()))
                .thenThrow(new CustomException(ErrorCode.RETURN_REPORT_FORBIDDEN));
        mockMvc.perform(authed(json(put(URL + "/5"), "{\"courses\":[]}")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("E403"))
                .andExpect(jsonPath("$.message").value("본인의 귀국보고서만 수정할 수 있습니다"));

        when(returnReportService.update(eq(1L), eq(6L), any()))
                .thenThrow(new CustomException(ErrorCode.RETURN_REPORT_NOT_FOUND));
        mockMvc.perform(authed(json(put(URL + "/6"), "{\"courses\":[]}")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("E404"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 귀국보고서입니다"));
    }

    @Test
    void invalidReportIdReturns400() throws Exception {
        mockMvc.perform(authed(delete(URL + "/abc")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E400"));
        verifyNoInteractions(returnReportService);
    }

    // ===== 삭제 =====

    @Test
    void deleteReturns200WithNullData() throws Exception {
        mockMvc.perform(authed(delete(URL + "/5")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("S200"))
                .andExpect(jsonPath("$.message").value("귀국보고서가 삭제되었습니다."))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.errors").doesNotExist());
        verify(returnReportService).delete(1L, 5L);
    }

    @Test
    void deleteForbiddenUsesDeleteMessage() throws Exception {
        doThrow(new CustomException(ErrorCode.RETURN_REPORT_FORBIDDEN, "본인의 귀국보고서만 삭제할 수 있습니다"))
                .when(returnReportService).delete(1L, 5L);

        mockMvc.perform(authed(delete(URL + "/5")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("본인의 귀국보고서만 삭제할 수 있습니다"));
    }
}
