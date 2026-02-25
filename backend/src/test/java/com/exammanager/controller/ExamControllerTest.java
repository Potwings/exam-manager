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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ExamControllerTest {

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
    void create_코드에디터_언어설정_포함() throws Exception {
        Map<String, Object> payload = Map.of(
                "title", "코드 언어 테스트",
                "problems", List.of(
                        Map.of(
                                "problemNumber", 1,
                                "content", "SQL 문제",
                                "contentType", "TEXT",
                                "codeEditor", true,
                                "codeLanguage", "sql",
                                "answerContent", "SELECT * FROM users",
                                "score", 10
                        ),
                        Map.of(
                                "problemNumber", 2,
                                "content", "일반 문제",
                                "contentType", "TEXT",
                                "codeEditor", false,
                                "answerContent", "답안",
                                "score", 5
                        )
                )
        );

        MvcResult result = mockMvc.perform(post("/api/exams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn();

        Long examId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        // 생성된 시험의 상세를 조회하여 codeLanguage 검증
        mockMvc.perform(get("/api/exams/" + examId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.problems[0].codeLanguage").value("sql"))
                .andExpect(jsonPath("$.problems[0].codeEditor").value(true))
                .andExpect(jsonPath("$.problems[1].codeLanguage").doesNotExist());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void create_그룹문제_하위_코드언어설정() throws Exception {
        Map<String, Object> payload = Map.of(
                "title", "그룹 코드 언어 테스트",
                "problems", List.of(
                        Map.ofEntries(
                                Map.entry("problemNumber", 1),
                                Map.entry("content", "공통 지문"),
                                Map.entry("contentType", "TEXT"),
                                Map.entry("codeEditor", false),
                                Map.entry("children", List.of(
                                        Map.of(
                                                "problemNumber", 1,
                                                "content", "Python 문제",
                                                "contentType", "TEXT",
                                                "codeEditor", true,
                                                "codeLanguage", "python",
                                                "answerContent", "print('hello')",
                                                "score", 5
                                        ),
                                        Map.of(
                                                "problemNumber", 2,
                                                "content", "JavaScript 문제",
                                                "contentType", "TEXT",
                                                "codeEditor", true,
                                                "codeLanguage", "javascript",
                                                "answerContent", "console.log('hello')",
                                                "score", 5
                                        )
                                ))
                        )
                )
        );

        MvcResult result = mockMvc.perform(post("/api/exams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andReturn();

        Long examId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/exams/" + examId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.problems[0].children[0].codeLanguage").value("python"))
                .andExpect(jsonPath("$.problems[0].children[1].codeLanguage").value("javascript"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updateProblem_코드언어_변경() throws Exception {
        // 시험 생성
        Map<String, Object> payload = Map.of(
                "title", "수정 테스트",
                "problems", List.of(
                        Map.of(
                                "problemNumber", 1,
                                "content", "Java 문제",
                                "contentType", "TEXT",
                                "codeEditor", true,
                                "codeLanguage", "java",
                                "answerContent", "System.out.println()",
                                "score", 10
                        )
                )
        );

        MvcResult createResult = mockMvc.perform(post("/api/exams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andReturn();

        Long examId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        // 문제 ID 조회
        MvcResult detailResult = mockMvc.perform(get("/api/exams/" + examId))
                .andExpect(status().isOk())
                .andReturn();

        Long problemId = objectMapper.readTree(detailResult.getResponse().getContentAsString())
                .get("problems").get(0).get("id").asLong();

        // codeLanguage를 sql로 변경
        Map<String, Object> updatePayload = Map.of(
                "content", "SQL 문제로 변경",
                "contentType", "TEXT",
                "codeEditor", true,
                "codeLanguage", "sql",
                "answerContent", "SELECT 1",
                "score", 10
        );

        mockMvc.perform(patch("/api/exams/" + examId + "/problems/" + problemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codeLanguage").value("sql"))
                .andExpect(jsonPath("$.content").value("SQL 문제로 변경"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void create_codeLanguage_미설정시_null() throws Exception {
        Map<String, Object> payload = Map.of(
                "title", "언어 미설정 테스트",
                "problems", List.of(
                        Map.of(
                                "problemNumber", 1,
                                "content", "코드 에디터 문제",
                                "contentType", "TEXT",
                                "codeEditor", true,
                                "answerContent", "답안",
                                "score", 10
                        )
                )
        );

        MvcResult result = mockMvc.perform(post("/api/exams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andReturn();

        Long examId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        // codeLanguage 미설정 시 null 반환 (프론트엔드에서 java로 폴백)
        mockMvc.perform(get("/api/exams/" + examId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.problems[0].codeEditor").value(true))
                .andExpect(jsonPath("$.problems[0].codeLanguage").doesNotExist());
    }
}
