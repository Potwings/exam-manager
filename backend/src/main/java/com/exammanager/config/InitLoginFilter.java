package com.exammanager.config;

import com.exammanager.entity.Admin;
import com.exammanager.repository.AdminRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@RequiredArgsConstructor
public class InitLoginFilter extends OncePerRequestFilter {

    private final AdminRepository adminRepository;

    private static final Set<String> ALLOWED_PATHS = Set.of(
            "/api/admin/change-password",
            "/api/admin/me",
            "/api/admin/logout"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (!path.startsWith("/api/") || ALLOWED_PATHS.contains(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            filterChain.doFilter(request, response);
            return;
        }

        Admin admin = adminRepository.findByUsername(auth.getName()).orElse(null);
        if (admin != null && admin.isInitLogin()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"message\":\"비밀번호 변경이 필요합니다\",\"initLogin\":true}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
