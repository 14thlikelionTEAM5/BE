package com.likelion14th._Team.domain.university.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.likelion14th._Team.domain.university.dto.UniversitySearch;
import com.likelion14th._Team.domain.university.entity.University;
import com.likelion14th._Team.domain.university.service.UniversityService;
import com.likelion14th._Team.domain.university.service.UniversityQueryService;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Transactional
class UniversityNameSearchIntegrationTests {
    @Autowired EntityManager em;
    @Autowired UniversityService autocomplete;
    @Autowired UniversityQueryRepository listRepository;
    @Autowired UniversityQueryService listService;
    @Autowired WebApplicationContext context;
    private String key;

    @BeforeEach
    void setUp() {
        key = UUID.randomUUID().toString().replace("-", "");
    }

    @Test
    void bothSearchesIgnoreCaseAndAccentsRegardlessOfDefaultCollation() {
        University accented = school(key + " École East");
        University plain = school(key + " Ecole West");
        assertMatches(key + " éCOLE", accented.getId(), plain.getId());
        assertMatches(key + " eCOLE", accented.getId(), plain.getId());
        List<Long> expectedOrder = List.of(accented.getId(), plain.getId());
        assertEquals(expectedOrder, autocomplete.search(key + " ecole").stream()
                .map(item -> item.universityId()).toList());
        for (String sort : List.of("NAME", "COUNTRY")) {
            assertEquals(expectedOrder, listService.list(Long.MAX_VALUE,
                    UniversitySearch.from(key + " ecole", null, null, null, sort)).universities()
                    .stream().map(item -> item.universityId()).toList());
        }
        assertMatches(key + " unknown");
    }

    @Test
    void bothSearchesTreatPercentUnderscoreAndBackslashLiterally() {
        University literal = school(key + " 100%_\\ North");
        school(key + " 100AA North");
        University exclamation = school(key + " !marker");
        assertMatches(key + " !marker", exclamation.getId());
        for (String suffix : List.of(" 100%", " 100%_", " 100%_\\")) {
            assertMatches(key + suffix, literal.getId());
        }
    }

    @Test
    void autocompleteSortsBeforeApplyingLimitAndListRetainsAllCandidates() {
        for (int index = 11; index >= 0; index--) {
            school(key + " Campus " + String.format("%02d", index));
        }
        em.flush();
        em.clear();
        List<String> expected = IntStream.range(0, 12)
                .mapToObj(index -> key + " Campus " + String.format("%02d", index)).toList();
        assertEquals(expected.subList(0, 10), autocomplete.search(key + " Campus")
                .stream().map(item -> item.name()).toList());
        assertEquals(12, listRepository.schools(Long.MAX_VALUE,
                UniversitySearch.from(key + " Campus", null, null, null, null), null).size());
        assertEquals(expected, listService.list(Long.MAX_VALUE,
                UniversitySearch.from(key + " Campus", null, null, null, null)).universities()
                .stream().map(item -> item.name()).toList());
    }

    @Test
    void autocompleteHandlesUnicodeEdgeWhitespaceAndReturnsEmptyForUnmatchedLiteral() {
        University university = school(key + " Campus");
        assertEquals(university.getId(), autocomplete.search("\u2003 " + key + " Campus \u2003")
                .getFirst().universityId());
        assertTrue(autocomplete.search(key + " %missing").isEmpty());
    }

    @Test
    void generatedSwaggerDocumentsSearchFailureAndAlphabeticallyOrderedExample() throws Exception {
        MockMvcBuilders.webAppContextSetup(context).build().perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/universities/search'].get.responses['500'].description")
                        .value("학교 검색 중 오류가 발생했습니다."))
                .andExpect(jsonPath("$.paths['/api/v1/universities/search'].get.responses['200'].content['application/json'].example.data[0].name")
                        .value("Nanyang Technological University"))
                .andExpect(jsonPath("$.paths['/api/v1/universities/search'].get.responses['200'].content['application/json'].example.data[1].name")
                        .value("National University of Singapore"));
    }

    private University school(String name) {
        University university = University.builder().name(name).country("테스트").build();
        em.persist(university);
        em.flush();
        return university;
    }

    private void assertMatches(String keyword, Long... expectedIds) {
        assertEquals(List.of(expectedIds), autocomplete.search(keyword).stream()
                .map(item -> item.universityId()).sorted().toList());
        assertEquals(List.of(expectedIds), listRepository.schools(Long.MAX_VALUE,
                UniversitySearch.from(keyword, null, null, null, null), null).stream()
                .map(row -> row.university().getId()).sorted().toList());
    }
}
