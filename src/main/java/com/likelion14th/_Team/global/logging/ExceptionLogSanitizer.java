package com.likelion14th._Team.global.logging;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** 요청값이 포함될 수 있는 예외 메시지를 제외하고 예외 종류와 호출 스택을 남긴다. */
public final class ExceptionLogSanitizer {
    private ExceptionLogSanitizer() {}

    public static Throwable withoutMessages(Throwable exception) {
        return copy(exception, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private static Throwable copy(Throwable exception, Set<Throwable> visited) {
        if (exception == null || !visited.add(exception)) return null;
        Throwable safe = new Throwable(exception.getClass().getName());
        safe.setStackTrace(exception.getStackTrace());
        Throwable cause = copy(exception.getCause(), visited);
        if (cause != null) safe.initCause(cause);
        for (Throwable suppressed : exception.getSuppressed()) {
            Throwable safeSuppressed = copy(suppressed, visited);
            if (safeSuppressed != null) safe.addSuppressed(safeSuppressed);
        }
        return safe;
    }
}
