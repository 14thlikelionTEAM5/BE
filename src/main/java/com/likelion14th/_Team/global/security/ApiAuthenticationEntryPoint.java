package com.likelion14th._Team.global.security;

import com.likelion14th._Team.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.RequestMatcher;

/** 경로별 401 응답을 등록 순서대로 결정한다. 접근 허용 여부는 SecurityConfig에서 관리한다. */
public final class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {
    public record Route(RequestMatcher matcher, ErrorCode errorCode) {}

    private final List<Route> routes;

    public ApiAuthenticationEntryPoint(List<Route> routes) {
        this.routes = List.copyOf(routes);
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        SecurityErrorWriter.write(response, resolveError(request));
    }

    private ErrorCode resolveError(HttpServletRequest request) {
        for (Route route : routes) {
            if (route.matcher().matches(request)) return route.errorCode();
        }
        return ErrorCode.UNAUTHORIZED;
    }
}
