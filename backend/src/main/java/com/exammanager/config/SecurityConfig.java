package com.exammanager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpStatus;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.exammanager.repository.AdminRepository;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AdminRepository adminRepository;

    public SecurityConfig(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // 관리자 인증 엔드포인트 — 인증 없이 접근 허용
                        .requestMatchers("/api/admin/login", "/api/admin/me").permitAll()
                        // 수험자용 공개 엔드포인트
                        .requestMatchers(HttpMethod.GET, "/api/exams/active").permitAll()
                        .requestMatchers("/api/examinees/**").permitAll()
                        .requestMatchers("/api/exam-sessions/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/submissions").permitAll()
                        // 관리자 전용 엔드포인트
                        .requestMatchers("/api/notifications/**").authenticated()
                        .requestMatchers("/api/exams/**").authenticated()
                        .requestMatchers("/api/submissions/**").authenticated()
                        .requestMatchers("/api/scores/**").authenticated()
                        .requestMatchers("/api/ai-assist/**").authenticated()
                        .requestMatchers("/api/admin/**").authenticated()
                        // 그 외 모든 요청 허용 (정적 리소스 등)
                        .anyRequest().permitAll()
                )
                .exceptionHandling(ex -> ex
                        // 미인증 시 로그인 폼 리다이렉트 대신 401 반환 (SPA 호환)
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .maximumSessions(1)
                )
                .addFilterAfter(new InitLoginFilter(adminRepository), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
        config.setAllowedHeaders(List.of("*"));
        // 세션 쿠키(JSESSIONID) 전송을 허용하기 위해 credentials를 true로 설정
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
