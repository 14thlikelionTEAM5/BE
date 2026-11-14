package com.likelion14th._Team.domain.university.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.likelion14th._Team.domain.university.dto.UniversitySearch;
import com.likelion14th._Team.global.exception.CustomException;
import org.junit.jupiter.api.Test;

class UniversitySearchKeywordTests {
    @Test
    void listAllowsOmittedOrBlankKeywordAsNoFilter() {
        assertNull(search(null).keyword());
        assertEquals("", search("").keyword());
        assertEquals("", search(" \t\n\u2003").keyword());
    }

    @Test
    void listStripsEdgesAndPreservesInternalSpacesAndLiteralCharacters() {
        assertEquals("Alpha  100%_\\", search("\u2003 Alpha  100%_\\ \u2003").keyword());
    }

    @Test
    void listLimitsNormalizedKeywordToTwoHundredCodePoints() {
        assertEquals("가".repeat(200), search(" " + "가".repeat(200) + " ").keyword());
        assertEquals("😀".repeat(200), search("😀".repeat(200)).keyword());
        assertThrows(CustomException.class, () -> search("가".repeat(201)));
        assertThrows(CustomException.class, () -> search("😀".repeat(201)));
    }

    private UniversitySearch search(String keyword) {
        return UniversitySearch.from(keyword, null, null, null, null);
    }
}
