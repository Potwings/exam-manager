package com.exammanager.controller;

import com.exammanager.entity.Admin;
import com.exammanager.repository.AdminRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // InitLoginFilter 통과를 위해 테스트용 admin을 initLogin=false로 생성
        if (adminRepository.findByUsername("admin").isEmpty()) {
            adminRepository.save(Admin.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .role("ADMIN")
                    .initLogin(false)
                    .build());
        } else {
            Admin admin = adminRepository.findByUsername("admin").get();
            admin.setInitLogin(false);
            adminRepository.save(admin);
        }
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void register_성공() throws Exception {
        mockMvc.perform(post("/api/admin/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "newadmin", "password", "pass1234"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("newadmin"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.initLogin").value(true));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void register_중복아이디_409() throws Exception {
        adminRepository.save(Admin.builder()
                .username("duplicate")
                .password(passwordEncoder.encode("pass1234"))
                .role("ADMIN")
                .build());

        mockMvc.perform(post("/api/admin/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "duplicate", "password", "pass1234"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("이미 존재하는 아이디입니다"));
    }

    @Test
    void register_미인증_401() throws Exception {
        mockMvc.perform(post("/api/admin/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "newadmin", "password", "pass1234"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void list_관리자목록_조회() throws Exception {
        mockMvc.perform(get("/api/admin/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void delete_다른관리자_삭제_성공() throws Exception {
        Admin target = adminRepository.save(Admin.builder()
                .username("target")
                .password(passwordEncoder.encode("pass1234"))
                .role("ADMIN")
                .build());

        mockMvc.perform(delete("/api/admin/" + target.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("삭제되었습니다"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void delete_자기자신_삭제불가_400() throws Exception {
        Admin self = adminRepository.findByUsername("admin").orElseThrow();

        mockMvc.perform(delete("/api/admin/" + self.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("자기 자신은 삭제할 수 없습니다"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void delete_존재하지않는_관리자_404() throws Exception {
        mockMvc.perform(delete("/api/admin/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("관리자를 찾을 수 없습니다"));
    }
}
