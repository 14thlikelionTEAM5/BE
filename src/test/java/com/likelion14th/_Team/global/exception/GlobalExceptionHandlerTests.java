package com.likelion14th._Team.global.exception;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTests {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void unhandledExceptionIs500WithoutLoggingExceptionMessage(CapturedOutput output) {
        String marker = UUID.randomUUID().toString();
        Exception exception = new IllegalStateException(marker, new IllegalArgumentException(marker));

        var response = handler.handleException(exception, new MockHttpServletRequest());

        assertTrue(response.getStatusCode().isSameCodeAs(HttpStatus.INTERNAL_SERVER_ERROR));
        assertFalse(output.getAll().contains(marker));
        assertTrue(output.getAll().contains("at " + getClass().getName()));
        assertTrue(output.getAll().contains("Caused by:"));
        assertTrue(output.getAll().contains("java.lang.IllegalArgumentException"));
    }
}