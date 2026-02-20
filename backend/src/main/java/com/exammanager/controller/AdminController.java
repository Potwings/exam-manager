package com.exammanager.controller;

import com.exammanager.dto.AdminLoginRequest;
import com.exammanager.dto.AdminResponse;
import com.exammanager.entity.Admin;
import com.exammanager.repository.AdminRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AuthenticationManager authenticationManager;
    private final AdminRepository adminRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AdminLoginRequest request, HttpServletRequest httpRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            // 프로그래매틱 인증에서는 SecurityContext를 세션에 수동 저장해야 함
            HttpSession session = httpRequest.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

            Admin admin = adminRepository.findByUsername(request.getUsername()).orElseThrow();
            return ResponseEntity.ok(AdminResponse.from(admin));
        } catch (Exception e) {
            log.warn("관리자 로그인 실패: {}", request.getUsername());
            return ResponseEntity.status(401).body(Map.of("message", "아이디 또는 비밀번호가 올바르지 않습니다"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("message", "로그아웃되었습니다"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(401).body(Map.of("message", "인증되지 않았습니다"));
        }

        String username = auth.getName();
        Admin admin = adminRepository.findByUsername(username).orElse(null);
        if (admin == null) {
            return ResponseEntity.status(401).body(Map.of("message", "인증되지 않았습니다"));
        }

        return ResponseEntity.ok(AdminResponse.from(admin));
    }
}
