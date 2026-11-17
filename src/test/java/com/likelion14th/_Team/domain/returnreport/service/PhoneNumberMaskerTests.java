package com.likelion14th._Team.domain.returnreport.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PhoneNumberMaskerTests {

    @Test
    void masksMobileAndLandlineNumbers() {
        assertEquals("연락: [연락처], [연락처], [연락처], [연락처]",
                PhoneNumberMasker.mask("연락: 010-1234-5678, 01012345678, 02-123-4567, 031-1234-5678"));
    }

    @Test
    void keepsOtherNumbers() {
        String text = "2026년 가을학기, 총 1,200만원, 학점 4.0, 학번 202012345";
        assertEquals(text, PhoneNumberMasker.mask(text));
        assertFalse(PhoneNumberMasker.containsPhoneNumber(text));
    }

    @Test
    void doesNotMaskPartOfLongerDigitRun() {
        assertEquals("주문번호 9010123456789", PhoneNumberMasker.mask("주문번호 9010123456789"));
    }

    @Test
    void detectsPhoneNumber() {
        assertTrue(PhoneNumberMasker.containsPhoneNumber("문의는 010-9876-5432로 해요"));
    }
}
