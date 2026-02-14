package com.exammanager.service;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DocxParserServiceTest {

    private DocxParserService service;
    private static final Path DOCS_DIR = Paths.get("../docs");

    @BeforeEach
    void setUp() {
        service = new DocxParserService();
    }

    @Test
    void parseProblems_shouldExtractAllQuestions() throws Exception {
        try (FileInputStream fis = new FileInputStream(DOCS_DIR.resolve("test_problem.docx").toFile());
             XWPFDocument doc = new XWPFDocument(fis)) {

            List<Map<String, String>> problems = service.extractProblems(doc);

            // 디버깅용 출력
            for (Map<String, String> p : problems) {
                String content = p.get("content");
                String firstLine = content.contains("\n") ? content.substring(0, content.indexOf("\n")) : content;
                System.out.printf("문제 %s: %s%n", p.get("number"), firstLine);
            }

            assertFalse(problems.isEmpty(), "문제가 하나 이상 추출되어야 함");

            // 첫 번째 문제 검증
            assertEquals("1", problems.get(0).get("number"));
            assertTrue(problems.get(0).get("content").contains("OOP"));

            // 모든 문제에 number와 content가 있어야 함
            for (Map<String, String> p : problems) {
                assertNotNull(p.get("number"));
                assertNotNull(p.get("content"));
                assertFalse(p.get("content").isEmpty());
            }
        }
    }

    @Test
    void parseAnswers_shouldExtract14Answers() throws Exception {
        try (FileInputStream fis = new FileInputStream(DOCS_DIR.resolve("test_answer.docx").toFile());
             XWPFDocument doc = new XWPFDocument(fis)) {

            List<Map<String, String>> answers = service.extractAnswers(doc);

            // 디버깅용 출력
            for (Map<String, String> a : answers) {
                String content = a.get("content");
                String firstLine = content.contains("\n") ? content.substring(0, content.indexOf("\n")) : content;
                System.out.printf("답안 %s (배점: %s): %s%n", a.get("number"), a.get("score"), firstLine);
            }

            assertEquals(14, answers.size(), "답안 14개 추출되어야 함");

            // 문제 번호 순서 검증
            for (int i = 0; i < 14; i++) {
                assertEquals(String.valueOf(i + 1), answers.get(i).get("number"));
            }

            // 배점 검증
            assertEquals("8", answers.get(0).get("score"));   // Q1: 총 8점
            assertEquals("6", answers.get(1).get("score"));   // Q2: 총 6점
            assertEquals("6", answers.get(2).get("score"));   // Q3: 총 6점
            assertEquals("6", answers.get(4).get("score"));   // Q5: 총 6점
            assertEquals("6", answers.get(7).get("score"));   // Q8: 총 6점
            assertEquals("5", answers.get(9).get("score"));   // Q10: 총 5점
            assertEquals("3", answers.get(11).get("score"));  // Q12: 총 3점
            assertEquals("10", answers.get(13).get("score")); // Q14: 10점

            // Q13 배점: 하위 질문 합산 (3+2+2+5+10 = 22)
            int q13Score = Integer.parseInt(answers.get(12).get("score"));
            assertEquals(22, q13Score, "Q13 하위 질문 배점 합산");

            // 답안 내용 검증
            assertTrue(answers.get(0).get("content").contains("추상화"));
            assertTrue(answers.get(0).get("content").contains("캡슐화"));
        }
    }

    @Test
    void parseAnswers_shouldContainAnswerContent() throws Exception {
        try (FileInputStream fis = new FileInputStream(DOCS_DIR.resolve("test_answer.docx").toFile());
             XWPFDocument doc = new XWPFDocument(fis)) {

            List<Map<String, String>> answers = service.extractAnswers(doc);

            // Q2: Overloading/Overriding 답안 확인
            String q2 = answers.get(1).get("content");
            assertTrue(q2.contains("Overloading"), "Q2 답안에 Overloading 포함");
            assertTrue(q2.contains("Overriding"), "Q2 답안에 Overriding 포함");

            // Q9: 코드 실행 결과 답안
            String q9 = answers.get(8).get("content");
            assertTrue(q9.contains("오류"), "Q9 답안에 오류 관련 내용 포함");

            // Q12: async/await 관련
            String q12 = answers.get(11).get("content");
            assertTrue(q12.contains("비동기"), "Q12 답안에 비동기 관련 내용 포함");
        }
    }
}
