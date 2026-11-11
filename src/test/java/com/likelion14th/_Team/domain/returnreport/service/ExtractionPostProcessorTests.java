package com.likelion14th._Team.domain.returnreport.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.likelion14th._Team.domain.returnreport.ai.LlmException;
import com.likelion14th._Team.domain.returnreport.dto.ReturnReportExtractResponse;
import com.likelion14th._Team.domain.returnreport.dto.ReturnReportExtractResponse.ExtractedCourse;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExtractionPostProcessorTests {

    private static final String SOURCE = """
            2026년 가을학기 싱가포르 교환. 항공권은 왕복 약 90만원, 보험료 약 30만원(삼성화재).
            월 생활비는 월 150만원 정도였고 총 1,200만원 들었어요. 기숙사(Hall) 생활.
            비자는 Student Pass, 발급까지 약 2주 걸림.
            수강: MKT2101 Principles of Marketing 4학점(만족도 4), FIN-2201 corporate finance 4학점.
            """;

    private final ExtractionPostProcessor processor = new ExtractionPostProcessor();

    private static String json(String body) {
        return body;
    }

    private static final String FULL = """
            {
              "cost": {"airfare": "왕복 약 90만원", "insurance": "약 30만원", "monthlyLiving": "월 150만원", "total": "총 1,200만원"},
              "housing": "DORM",
              "visa": {"duration": "약 2주", "type": "Student Pass"},
              "insuranceProvider": "삼성화재",
              "courses": [
                {"code": "MKT2101", "nameEn": "Principles of Marketing", "foreignCredit": 4, "satisfaction": 4},
                {"code": "FIN2201", "nameEn": "Corporate Finance", "foreignCredit": 4.0, "satisfaction": null}
              ],
              "summary": "기숙사에서 지냈고 총 1,200만원이 들었어요."
            }
            """;

    @Test
    void mapsInternalSchemaToApiFields() {
        ExtractionPostProcessor.Result result = processor.process(FULL, SOURCE);
        ReturnReportExtractResponse response = result.response();

        assertEquals("왕복 약 90만원", response.cost().flight());
        assertEquals("약 30만원", response.cost().insurance());
        assertEquals("월 150만원", response.cost().livingMonthly());
        assertEquals("총 1,200만원", response.cost().total());
        assertEquals("DORM", response.housingType());
        assertEquals("약 2주", response.visa().duration());
        assertEquals("Student Pass", response.visa().type());
        assertEquals("삼성화재", response.insuranceProvider());
        assertEquals(List.of(
                new ExtractedCourse("MKT2101", "Principles of Marketing", new BigDecimal("4.0"), 4),
                new ExtractedCourse("FIN2201", "Corporate Finance", new BigDecimal("4.0"), null)),
                response.courses());
        assertEquals("기숙사에서 지냈고 총 1,200만원이 들었어요.", response.reviewSummary());
        assertEquals(0, result.droppedFields());
    }

    @Test
    void sourceComparisonIgnoresSpacesPunctuationAndCase() {
        // FIN2201 ↔ 원문 FIN-2201, Corporate Finance ↔ 원문 corporate finance
        ReturnReportExtractResponse response = processor.process(FULL, SOURCE).response();

        assertEquals("FIN2201", response.courses().get(1).courseCode());
    }

    @Test
    void dropsValuesNotInSource() {
        String body = """
                {"cost": {"airfare": "왕복 200만원", "insurance": null, "monthlyLiving": null, "total": null},
                 "housing": null, "visa": {"duration": null, "type": "관광비자"}, "insuranceProvider": "현대해상",
                 "courses": [
                   {"code": "XYZ999", "nameEn": "Principles of Marketing", "foreignCredit": 4, "satisfaction": 4},
                   {"code": "MKT2101", "nameEn": "Data Science", "foreignCredit": 3, "satisfaction": 5}
                 ],
                 "summary": null}
                """;
        ExtractionPostProcessor.Result result = processor.process(body, SOURCE);
        ReturnReportExtractResponse response = result.response();

        assertNull(response.cost().flight());
        assertNull(response.visa().type());
        assertNull(response.insuranceProvider());
        assertEquals(1, response.courses().size());
        assertNull(response.courses().getFirst().courseCode());
        assertEquals("Principles of Marketing", response.courses().getFirst().courseNameEn());
        assertEquals(5, result.droppedFields());
    }

    @Test
    void appliesSaveApiConstraints() {
        String longCost = "약".repeat(201);
        String source = SOURCE + longCost;
        String body = """
                {"cost": {"airfare": "%s", "insurance": null, "monthlyLiving": null, "total": null},
                 "housing": "dorm", "visa": null, "insuranceProvider": null,
                 "courses": [
                   {"code": "MKT2101", "nameEn": "Principles of Marketing", "foreignCredit": 0, "satisfaction": 6},
                   {"code": "MKT2101", "nameEn": "Principles of Marketing", "foreignCredit": 100, "satisfaction": 0},
                   {"code": "MKT2101", "nameEn": "Principles of Marketing", "foreignCredit": 4.25, "satisfaction": 4.5},
                   {"code": "MKT2101", "nameEn": "Principles of Marketing", "foreignCredit": "4", "satisfaction": "4"},
                   {"code": "MKT2101", "nameEn": "Principles of Marketing", "foreignCredit": 99.9, "satisfaction": 5.0}
                 ],
                 "summary": null}
                """.formatted(longCost);
        ReturnReportExtractResponse response = processor.process(body, source).response();

        assertNull(response.cost().flight());
        assertNull(response.housingType());
        assertNotNull(response.visa());
        assertNull(response.visa().type());
        for (int i = 0; i < 4; i++) {
            assertNull(response.courses().get(i).foreignCredit());
            assertNull(response.courses().get(i).satisfaction());
        }
        assertEquals(new BigDecimal("99.9"), response.courses().get(4).foreignCredit());
        assertEquals(5, response.courses().get(4).satisfaction());
    }

    @Test
    void trimsAndTreatsBlankAsNull() {
        String body = """
                {"cost": {"airfare": "  왕복 약 90만원  ", "insurance": "   ", "monthlyLiving": "", "total": null},
                 "housing": " DORM ", "visa": {"duration": null, "type": null}, "insuranceProvider": null,
                 "courses": [{"code": " ", "nameEn": "  ", "foreignCredit": 4, "satisfaction": null}],
                 "summary": "  "}
                """;
        ReturnReportExtractResponse response = processor.process(body, SOURCE).response();

        assertEquals("왕복 약 90만원", response.cost().flight());
        assertNull(response.cost().insurance());
        assertNull(response.cost().livingMonthly());
        assertEquals("DORM", response.housingType());
        assertTrue(response.courses().isEmpty());
        assertNull(response.reviewSummary());
    }

    @Test
    void summaryIsClearedWhenTooLongOrHasPhoneOrUnknownNumber() {
        String[] summaries = {
                "가".repeat(501),
                "문의는 010-1234-5678로 했어요.",
                "총 3,500만원이 들었어요."};
        for (String summary : summaries) {
            String body = """
                    {"cost": null, "housing": null, "visa": null, "insuranceProvider": null, "courses": [],
                     "summary": "%s"}
                    """.formatted(summary);
            assertNull(processor.process(body, SOURCE).response().reviewSummary(), summary);
        }
    }

    @Test
    void emptyResultIsNotAnError() {
        String body = """
                {"cost": null, "housing": null, "visa": null, "insuranceProvider": null, "courses": [], "summary": null}
                """;
        ReturnReportExtractResponse response = processor.process(body, SOURCE).response();

        assertNotNull(response.cost());
        assertNull(response.cost().flight());
        assertNotNull(response.visa());
        assertTrue(response.courses().isEmpty());
    }

    @Test
    void skipsSourceCheckWhenSourceIsNull() {
        String body = """
                {"cost": {"airfare": "원문에 없는 값", "insurance": null, "monthlyLiving": null, "total": null},
                 "housing": null, "visa": null, "insuranceProvider": null,
                 "courses": [{"code": null, "nameEn": "Anything", "foreignCredit": 3, "satisfaction": null}],
                 "summary": "총 9,999만원이 들었어요."}
                """;
        ReturnReportExtractResponse response = processor.process(body, null).response();

        assertEquals("원문에 없는 값", response.cost().flight());
        assertEquals("Anything", response.courses().getFirst().courseNameEn());
        assertEquals("총 9,999만원이 들었어요.", response.reviewSummary());
    }

    @Test
    void invalidJsonOrMissingKeysIsParseFailure() {
        String[] bodies = {"not json", "[]", "{\"cost\": null}", "", null};
        for (String body : bodies) {
            LlmException e = assertThrows(LlmException.class, () -> processor.process(body, SOURCE));
            assertEquals(LlmException.Kind.PARSE_FAILED, e.getKind());
            assertNull(e.getCause());
        }
    }
}
