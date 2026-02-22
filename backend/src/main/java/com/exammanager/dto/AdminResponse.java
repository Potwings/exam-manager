package com.exammanager.dto;

import com.exammanager.entity.Admin;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AdminResponse {
    private Long id;
    private String username;
    private String role;
    private boolean initLogin;
    private LocalDateTime createdAt;

    public static AdminResponse from(Admin admin) {
        return AdminResponse.builder()
                .id(admin.getId())
                .username(admin.getUsername())
                .role(admin.getRole())
                .initLogin(admin.isInitLogin())
                .createdAt(admin.getCreatedAt())
                .build();
    }
}
