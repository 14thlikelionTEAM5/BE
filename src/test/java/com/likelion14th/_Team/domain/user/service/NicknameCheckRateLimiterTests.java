package com.likelion14th._Team.domain.user.service;

import org.junit.jupiter.api.Test;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class NicknameCheckRateLimiterTests {
    @Test void windowExpiresAfterOneMinute() {
        MutableClock clock = new MutableClock();
        NicknameCheckRateLimiter limiter = new NicknameCheckRateLimiter(clock);
        for (int i=0; i<30; i++) assertTrue(limiter.allow("ip"));
        assertFalse(limiter.allow("ip"));
        clock.millis=59_999;
        assertFalse(limiter.allow("ip"));
        clock.millis=60_000;
        assertTrue(limiter.allow("ip"));
    }
    private static class MutableClock extends Clock {
        long millis;
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return Instant.ofEpochMilli(millis); }
    }
}
