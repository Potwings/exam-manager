package com.exammanager.service;

import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;
import java.util.regex.*;

@Service
public class DocxParserService {

    private static final Pattern QUESTION_NUM = Pattern.compile("^(\\d+)\\.\\s*(.+)");
    private static final Pattern TOTAL_SCORE = Pattern.compile("\\(총\\s*(\\d+)점\\)");
    private static final Pattern SINGLE_SCORE = Pattern.compile("\\((\\d+)점\\)");
    private static final Pattern SUB_QUESTION = Pattern.compile("^[A-E]\\.\\s*.+");

    /**
     * 문제 docx 파일에서 문제 목록을 추출한다.
     * 각 Map은 "number"(문제 번호)와 "content"(문제 내용) 키를 가진다.
     */
    public List<Map<String, String>> parseProblems(MultipartFile file) {
        try (InputStream is = file.getInputStream();
             XWPFDocument doc = new XWPFDocument(is)) {
            return extractProblems(doc);
        } catch (Exception e) {
            throw new RuntimeException("문제 파일 파싱 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 답안 docx 파일에서 답안 목록을 추출한다.
     * 각 Map은 "number"(문제 번호), "content"(답안 내용), "score"(배점) 키를 가진다.
     */
    public List<Map<String, String>> parseAnswers(MultipartFile file) {
        try (InputStream is = file.getInputStream();
             XWPFDocument doc = new XWPFDocument(is)) {
            return extractAnswers(doc);
        } catch (Exception e) {
            throw new RuntimeException("답안 파일 파싱 실패: " + e.getMessage(), e);
        }
    }

    // ===== 문제 파싱 =====

    List<Map<String, String>> extractProblems(XWPFDocument doc) {
        List<Map<String, String>> problems = new ArrayList<>();
        int questionNum = 0;
        StringBuilder content = null;
        String prevNonEmpty = null;
        int emptyGap = 0;
        boolean inSubQuestionZone = false;

        for (IBodyElement el : doc.getBodyElements()) {
            if (el instanceof XWPFParagraph) {
                String text = ((XWPFParagraph) el).getText().trim();
                if (text.isEmpty()) {
                    emptyGap++;
                    continue;
                }

                if (isNewProblemStart(text, prevNonEmpty, emptyGap, inSubQuestionZone)) {
                    if (content != null) {
                        problems.add(makeEntry(questionNum, content.toString().trim()));
                    }
                    questionNum++;
                    content = new StringBuilder(text);
                    inSubQuestionZone = text.contains("하위 질문") || text.contains("하위질문");
                } else if (content != null) {
                    content.append("\n").append(text);
                }

                prevNonEmpty = text;
                emptyGap = 0;
            } else if (el instanceof XWPFTable) {
                if (content != null) {
                    content.append("\n").append(tableToText((XWPFTable) el));
                }
                emptyGap = 0;
            }
        }

        if (content != null) {
            problems.add(makeEntry(questionNum, content.toString().trim()));
        }
        return problems;
    }

    private boolean isNewProblemStart(String text, String prev, int gap, boolean subZone) {
        if (!isKoreanQuestion(text)) return false;
        if (prev == null) return true;
        if (gap >= 5) return true;
        if (isCodeLine(prev)) return true;
        if (subZone && !isSubQuestionRelated(text)) return true;
        return false;
    }

    private boolean isKoreanQuestion(String text) {
        if (text.length() < 10) return false;
        if (!text.matches(".*(?:세요|십시오)\\.?\\s*$")) return false;
        return text.chars().filter(c -> c >= 0xAC00 && c <= 0xD7AF).count() >= 5;
    }

    private boolean isCodeLine(String text) {
        return text.matches(".*[{}();]\\s*$");
    }

    private boolean isSubQuestionRelated(String text) {
        return text.contains("쿼리") || text.contains("사원") || text.contains("제약사항");
    }

    // ===== 답안 파싱 =====

    List<Map<String, String>> extractAnswers(XWPFDocument doc) {
        List<Map<String, String>> answers = new ArrayList<>();
        int currentNum = 0;
        int headerScore = 0;
        StringBuilder content = null;

        for (IBodyElement el : doc.getBodyElements()) {
            if (el instanceof XWPFParagraph) {
                XWPFParagraph para = (XWPFParagraph) el;
                String text = para.getText().trim();
                if (text.isEmpty()) continue;

                Matcher m = QUESTION_NUM.matcher(text);
                if (m.matches() && isAnswerHeader(para, Integer.parseInt(m.group(1)), currentNum)) {
                    if (content != null) {
                        int score = headerScore > 0 ? headerScore : sumSubScores(content.toString());
                        answers.add(makeAnswerEntry(currentNum, content.toString().trim(), score));
                    }
                    currentNum = Integer.parseInt(m.group(1));
                    headerScore = extractScore(m.group(2));
                    content = new StringBuilder();
                } else if (content != null) {
                    if (content.length() > 0) content.append("\n");
                    content.append(text);
                }
            } else if (el instanceof XWPFTable) {
                if (content != null) {
                    if (content.length() > 0) content.append("\n");
                    content.append(tableToText((XWPFTable) el));
                }
            }
        }

        if (content != null) {
            int score = headerScore > 0 ? headerScore : sumSubScores(content.toString());
            answers.add(makeAnswerEntry(currentNum, content.toString().trim(), score));
        }
        return answers;
    }

    private boolean isBold(XWPFParagraph para) {
        return !para.getRuns().isEmpty() && Boolean.TRUE.equals(para.getRuns().get(0).isBold());
    }

    private boolean isAnswerHeader(XWPFParagraph para, int parsedNum, int currentNum) {
        // bold이면 확실한 헤더
        if (isBold(para)) return true;
        // bold가 아니어도 다음 순번이면 헤더로 판단
        return parsedNum == currentNum + 1;
    }

    private int extractScore(String text) {
        Matcher m = TOTAL_SCORE.matcher(text);
        if (m.find()) return Integer.parseInt(m.group(1));
        m = SINGLE_SCORE.matcher(text);
        if (m.find()) return Integer.parseInt(m.group(1));
        return 0;
    }

    private int sumSubScores(String content) {
        int total = 0;
        for (String line : content.split("\n")) {
            if (SUB_QUESTION.matcher(line.trim()).matches()) {
                Matcher m = SINGLE_SCORE.matcher(line);
                if (m.find()) {
                    total += Integer.parseInt(m.group(1));
                }
            }
        }
        return total;
    }

    // ===== 유틸리티 =====

    private String tableToText(XWPFTable table) {
        StringBuilder sb = new StringBuilder("[표]\n");
        for (int r = 0; r < table.getNumberOfRows(); r++) {
            XWPFTableRow row = table.getRow(r);
            List<XWPFTableCell> cells = row.getTableCells();
            for (int c = 0; c < cells.size(); c++) {
                if (c > 0) sb.append(" | ");
                sb.append(cells.get(c).getText().replace("\n", " "));
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private Map<String, String> makeEntry(int number, String content) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("number", String.valueOf(number));
        map.put("content", content);
        return map;
    }

    private Map<String, String> makeAnswerEntry(int number, String content, int score) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("number", String.valueOf(number));
        map.put("content", content);
        map.put("score", String.valueOf(score));
        return map;
    }
}
