package com.exammanager.controller;

import com.exammanager.dto.AdminLoginRequest;
import com.exammanager.dto.AdminRegisterRequest;
import com.exammanager.dto.AdminResponse;
import com.exammanager.dto.ChangePasswordRequest;
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
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AuthenticationManager authenticationManager;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AdminLoginRequest request, HttpServletRequest httpRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            // Session Fixation 방지: 인증 성공 후 기존 세션을 무효화하고 새 세션을 발급
            HttpSession oldSession = httpRequest.getSession(false);
            if (oldSession != null) {
                oldSession.invalidate();
            }
            HttpSession session = httpRequest.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

            Admin admin = adminRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "관리자 정보 조회 실패"));
            return ResponseEntity.ok(AdminResponse.from(admin));
        } catch (AuthenticationException e) {
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

    @PatchMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                             HttpServletRequest httpRequest) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(401).body(Map.of("message", "인증되지 않았습니다"));
        }
        String username = auth.getName();

        Admin admin = adminRepository.findByUsername(username).orElse(null);
        if (admin == null) {
            return ResponseEntity.status(401).body(Map.of("message", "인증되지 않았습니다"));
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), admin.getPassword())) {
            return ResponseEntity.status(400).body(Map.of("message", "현재 비밀번호가 올바르지 않습니다"));
        }

        admin.setPassword(passwordEncoder.encode(request.getNewPassword()));
        admin.setInitLogin(false);
        adminRepository.save(admin);
        log.info("비밀번호 변경 완료: {}", username);

        // 세션 무효화 — 탈취된 세션 ID로의 접근을 차단하기 위해 비밀번호 변경 후 강제 로그아웃
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();

        return ResponseEntity.ok(Map.of("message", "비밀번호가 변경되었습니다. 다시 로그인해주세요."));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody AdminRegisterRequest request) {
        if (adminRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.status(409).body(Map.of("message", "이미 존재하는 아이디입니다"));
        }

        Admin admin = Admin.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role("ADMIN")
                .initLogin(true)
                .build();
        adminRepository.save(admin);
        log.info("관리자 등록 완료: {}", request.getUsername());
        return ResponseEntity.ok(AdminResponse.from(admin));
    }

    @GetMapping("/list")
    public ResponseEntity<List<AdminResponse>> list() {
        List<AdminResponse> admins = adminRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(AdminResponse::from)
                .toList();
        return ResponseEntity.ok(admins);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(401).body(Map.of("message", "인증되지 않았습니다"));
        }
        String currentUsername = auth.getName();

        Admin target = adminRepository.findById(id).orElse(null);
        if (target == null) {
            return ResponseEntity.status(404).body(Map.of("message", "관리자를 찾을 수 없습니다"));
        }

        if (target.getUsername().equals(currentUsername)) {
            return ResponseEntity.status(400).body(Map.of("message", "자기 자신은 삭제할 수 없습니다"));
        }

        adminRepository.delete(target);
        log.info("관리자 삭제 완료: {}", target.getUsername());
        return ResponseEntity.ok(Map.of("message", "삭제되었습니다"));
    }
}
