package com.likelion14th._Team.domain.university.service;

import com.likelion14th._Team.domain.university.repository.UniversityQueryRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UniversityCardAssemblerTests {
    private final UniversityQueryRepository repository = mock(UniversityQueryRepository.class);
    private final UniversityCardAssembler assembler = new UniversityCardAssembler(repository,
            new UniversityEligibilityCalculator());

    @Test
    void semesterSelectionKeepsNullAndCurrentOnlyAndComparisonScopes() {
        when(repository.semesters()).thenReturn(List.of());
        var empty = assembler.semesters();
        assertNull(empty.current());
        assertNull(empty.previous());
        assertTrue(empty.currentOnly().isEmpty());
        assertTrue(empty.comparison().isEmpty());
        when(repository.semesters()).thenReturn(List.of("2027-1"));
        var single = assembler.semesters();
        assertNull(single.previous());
        assertEquals(List.of("2027-1"), single.comparison());
        when(repository.semesters()).thenReturn(List.of("2027-1", "2026-2"));
        var compared = assembler.semesters();
        assertEquals(List.of("2027-1"), compared.currentOnly());
        assertEquals(List.of("2027-1", "2026-2"), compared.comparison());
    }

    @Test
    void noSchoolsSkipAllCardQueries() {
        assertTrue(assembler.cards(1L, List.of("2027-1"), List.of()).isEmpty());
        verifyNoInteractions(repository);
    }
}
