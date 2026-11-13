package com.likelion14th._Team.global.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.likelion14th._Team.domain.user.repository.UserRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * 컨트롤러 테스트용 설정.
 * JwtAuthenticationFilter가 토큰 사용자를 "DB에 존재하는 사용자"로 인식하도록
 * 항상 존재한다고 답하는 UserRepository를 등록한다.
 */
@TestConfiguration
public class ActiveUserTestConfig {

    @Bean
    UserRepository userRepository() {
        UserRepository repository = mock(UserRepository.class);
        when(repository.existsById(any())).thenReturn(true);
        return repository;
    }
}