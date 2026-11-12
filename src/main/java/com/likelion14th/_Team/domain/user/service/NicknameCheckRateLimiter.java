package com.likelion14th._Team.domain.user.service;

import org.springframework.stereotype.Component;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

/** 단일 서버의 IP별 고정 1분 창. 만료된 IP는 매 요청에서 제거한다. */
@Component
public class NicknameCheckRateLimiter {
    private final Clock clock;
    private final Map<String, Window> windows = new HashMap<>();

    public NicknameCheckRateLimiter() { this(Clock.systemUTC()); }
    NicknameCheckRateLimiter(Clock clock) { this.clock = clock; }

    public synchronized boolean allow(String ip) {
        long now = clock.millis();
        windows.values().removeIf(window -> now - window.startedAt >= 60_000);
        Window window = windows.computeIfAbsent(ip, key -> new Window(now));
        if (window.count >= 30) return false;
        window.count++;
        return true;
    }

    private static class Window {
        final long startedAt;
        int count;
        Window(long startedAt) { this.startedAt = startedAt; }
    }
}
