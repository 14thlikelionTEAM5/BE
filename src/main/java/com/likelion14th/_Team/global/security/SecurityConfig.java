package com.likelion14th._Team.global.security;

import com.likelion14th._Team.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** 로그인 없이 호출할 수 있는 API (회원가입, 로그인, 메일 인증, 닉네임 중복 확인) + Swagger */
    private static final String[] PUBLIC_URLS = {
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/api/v1/auth/**",
            "/api/v1/users/nickname/check"
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtProvider jwtProvider) throws Exception {
        var paths = PathPatternRequestMatcher.withDefaults();
        var admissionStats = paths.matcher(HttpMethod.GET, "/api/v1/universities/{universityId}/admission-reports/stats");
        var admissionReports = paths.matcher(HttpMethod.GET, "/api/v1/universities/{universityId}/admission-reports");
        var universities = paths.matcher(HttpMethod.GET, "/api/v1/universities");
        var universityCountries = paths.matcher(HttpMethod.GET, "/api/v1/universities/countries");
        var universityDetail = paths.matcher(HttpMethod.GET, "/api/v1/universities/{universityId}");
        var universityCourses = paths.matcher(HttpMethod.GET, "/api/v1/universities/{universityId}/courses");
        var studyPlanById = new OrRequestMatcher(
                paths.matcher(HttpMethod.POST, "/api/v1/study-plans/{studyPlanId}/items"),
                paths.matcher(HttpMethod.PATCH, "/api/v1/study-plans/{studyPlanId}/items/{itemId}"),
                paths.matcher(HttpMethod.DELETE, "/api/v1/study-plans/{studyPlanId}/items/{itemId}"),
                paths.matcher(HttpMethod.DELETE, "/api/v1/study-plans/{studyPlanId}/items"),
                paths.matcher(HttpMethod.GET, "/api/v1/study-plans/{studyPlanId}"),
                paths.matcher(HttpMethod.PATCH, "/api/v1/study-plans/{studyPlanId}"),
                paths.matcher(HttpMethod.PATCH, "/api/v1/study-plans"),
                paths.matcher(HttpMethod.DELETE, "/api/v1/study-plans/{studyPlanId}"),
                paths.matcher(HttpMethod.DELETE, "/api/v1/study-plans"));
        var studyPlans = new OrRequestMatcher(
                paths.matcher(HttpMethod.GET, "/api/v1/study-plans"),
                paths.matcher(HttpMethod.POST, "/api/v1/study-plans"));
        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .cors(Customizer.withDefaults())
                // 세션을 쓰지 않고 요청마다 토큰으로 본인 확인
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        .requestMatchers(admissionStats).authenticated()
                        .requestMatchers(admissionReports).authenticated()
                        .requestMatchers(universities).authenticated()
                        .requestMatchers(universityCountries).authenticated()
                        .requestMatchers(universityDetail).authenticated()
                        .requestMatchers(universityCourses).authenticated()
                        .requestMatchers(studyPlanById).authenticated()
                        .requestMatchers(studyPlans).authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/universities/search", "/api/v1/home-courses").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/consents").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/bookmarks").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/bookmarks").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/bookmarks/{universityId}").authenticated()
                        .requestMatchers("/api/v1/return-reports/**").authenticated()
                        .requestMatchers("/api/v1/course-cases/**").authenticated()
                        .requestMatchers("/api/v1/admission-reports/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/experiences/check-duplicate").authenticated()
                        // TODO: 로그인 API가 머지되면 permitAll()을 authenticated()로 바꾼다.
                        // 지금 바꾸면 로그인할 방법이 없어서 다른 API를 Swagger에서 테스트할 수 없다.
                        .anyRequest().permitAll())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(new ApiAuthenticationEntryPoint(List.of(
                                new ApiAuthenticationEntryPoint.Route(
                                        new OrRequestMatcher(universityCountries, universityDetail),
                                        ErrorCode.UNIVERSITY_COUNTRIES_LOGIN_REQUIRED),
                                new ApiAuthenticationEntryPoint.Route(universities, ErrorCode.UNIVERSITY_LOGIN_REQUIRED),
                                new ApiAuthenticationEntryPoint.Route(studyPlanById, ErrorCode.STUDY_PLAN_LOGIN_REQUIRED),
                                new ApiAuthenticationEntryPoint.Route(studyPlans, ErrorCode.AUTHENTICATION_REQUIRED))))
                        .accessDeniedHandler((request, response, e) ->
                                SecurityErrorWriter.write(response, ErrorCode.FORBIDDEN)))
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** FE(다른 주소)에서 API를 호출할 수 있도록 허용할 주소. application.yaml의 app.cors.allowed-origins */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") String[] allowedOrigins
    ) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
