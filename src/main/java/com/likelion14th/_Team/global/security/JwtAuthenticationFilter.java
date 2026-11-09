package com.likelion14th._Team.global.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 요청 헤더 "Authorization: Bearer 토큰"을 읽어 로그인 사용자를 확인하는 필터.
 * 토큰이 유효하면 컨트롤러에서 @AuthenticationPrincipal Long userId 로 userId를 받을 수 있다.
 * 토큰이 없거나 잘못됐으면 인증 정보 없이 통과시키고, 접근 허용 여부는 SecurityConfig가 결정한다.
 *
 * 주의: @Component를 붙이지 않는다. 붙이면 스프링이 이 필터를 한 번 더 등록해서 두 번 실행된다.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;

    public JwtAuthenticationFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            try {
                Long userId = jwtProvider.getUserId(token);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException e) {
                // 만료/위조/형식 오류 토큰은 로그인하지 않은 것으로 취급한다
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}