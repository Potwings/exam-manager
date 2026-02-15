package com.exammanager.service;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

class DocxDumpTest {

    private final DocxParserService service = new DocxParserService();
    private static final Path DOCS_DIR = Paths.get("../docs");

    @Test
    void dumpAll() throws Exception {
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream("../docs/dump.txt"), StandardCharsets.UTF_8))) {

            try (FileInputStream fis = new FileInputStream(DOCS_DIR.resolve("test_problem.docx").toFile());
                 XWPFDocument doc = new XWPFDocument(fis)) {
                List<Map<String, String>> problems = service.extractProblems(doc);
                pw.println("===== PROBLEMS =====");
                for (Map<String, String> p : problems) {
                    pw.println("--- Q" + p.get("number") + " ---");
                    pw.println(p.get("content"));
                    pw.println();
                }
            }

            try (FileInputStream fis = new FileInputStream(DOCS_DIR.resolve("test_answer.docx").toFile());
                 XWPFDocument doc = new XWPFDocument(fis)) {
                List<Map<String, String>> answers = service.extractAnswers(doc);
                pw.println("===== ANSWERS =====");
                for (Map<String, String> a : answers) {
                    pw.println("--- A" + a.get("number") + " (score: " + a.get("score") + ") ---");
                    pw.println(a.get("content"));
                    pw.println();
                }
            }
        }
    }
}
