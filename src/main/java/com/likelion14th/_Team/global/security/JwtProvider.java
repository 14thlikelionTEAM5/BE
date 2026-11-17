package com.likelion14th._Team.global.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT access 토큰 발급/검증.
 * 토큰의 subject에 userId를 넣고, 요청 때 꺼내서 "누가 보낸 요청인지" 확인한다.
 */
@Component
public class JwtProvider {

    private final SecretKey key;
    private final long expirationSeconds;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-seconds}") long expirationSeconds
    ) {
        // HS256 키는 32바이트(영문 32자) 이상이어야 한다
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationSeconds = expirationSeconds;
    }

    /** 로그인 성공 시 호출. 로그인 응답의 accessToken에 넣는다 */
    public String createAccessToken(Long userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationSeconds * 1000);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /** 로그인 응답의 expiresIn (초) */
    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    /**
     * 토큰을 검증하고 userId를 꺼낸다.
     * 서명이 틀렸거나 만료되었거나 형식이 잘못되면 JwtException(또는 IllegalArgumentException)이 발생한다.
     */
    public Long getUserId(String token) {
        String subject = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
        return Long.valueOf(subject);
    }
}