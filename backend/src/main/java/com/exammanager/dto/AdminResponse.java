package com.exammanager.dto;

import com.exammanager.entity.Admin;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AdminResponse {
    private Long id;
    private String username;
    private String role;

    public static AdminResponse from(Admin admin) {
        return AdminResponse.builder()
                .id(admin.getId())
                .username(admin.getUsername())
                .role(admin.getRole())
                .build();
    }
}
