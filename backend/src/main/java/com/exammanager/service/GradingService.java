package com.exammanager.service;

import com.exammanager.entity.Answer;
import com.exammanager.entity.Submission;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GradingService {

    private final OllamaClient ollamaClient;

    private static final String SYSTEM_PROMPT = """
            당신은 기술 면접 필기시험의 엄격한 채점관입니다. 수험자의 답안을 채점 기준에 따라 공정하고 엄격하게 채점하세요.

            채점 원칙:
            1. 채점 기준에 "설명"이 요구되는 문제는 반드시 설명이 있어야 해당 점수를 부여합니다.
            2. 용어나 키워드만 나열하고 설명이 없으면 채점 기준에 명시된 부분 점수만 부여하세요. 절대로 만점을 주지 마세요.
            3. 만점은 채점 기준의 모든 항목에 대해 정확한 설명이 포함된 경우에만 부여합니다.
            4. 정확한 표현이 아니더라도 의미가 동일하면 점수를 부여합니다.
            5. 답안이 비어있거나 관련 없는 내용이면 0점입니다.
            6. 코드 문제의 경우, 실행 결과가 정확해야 점수를 부여합니다.
            7. SQL 문제의 경우, 문법이 완벽하지 않아도 핵심 로직이 맞으면 부분 점수를 부여합니다.

            중요:
            - "설명하세요" 문제에서 단어만 나열한 답안은 만점이 아닌 부분 점수 처리합니다.
            - 채점 기준에 "이름만 나열하면 감점" 또는 "설명 없으면 N점" 같은 조건이 있으면 반드시 따르세요.
            - [필수 키워드]는 채점 시 중요하게 봐야 할 핵심 개념입니다.
            - 정확히 같은 단어가 아니어도 의미나 뉘앙스가 유사하면 인정합니다. (예: "클라이언트에 저장" = "브라우저에 저장" = "로컬에 저장")
            - 핵심 개념을 설명하고 있으면 표현이 달라도 점수를 부여하세요.
            - 핵심 개념 자체가 빠져있으면 해당 점수를 부여하지 마세요.

            반드시 아래 JSON 형식으로만 응답하세요:
            {"earnedScore": <0 이상 배점 이하의 정수>, "feedback": "<채점 근거를 간결하게 설명>"}
            """;

    public void grade(Submission submission, Answer answer) {
        if (answer == null) {
            submission.setIsCorrect(false);
            submission.setEarnedScore(0);
            submission.setFeedback("채점 기준이 등록되지 않은 문제입니다.");
            return;
        }

        String submittedAnswer = submission.getSubmittedAnswer();
        if (submittedAnswer == null || submittedAnswer.trim().isEmpty()) {
            submission.setIsCorrect(false);
            submission.setEarnedScore(0);
            submission.setFeedback("답안이 제출되지 않았습니다.");
            return;
        }

        int maxScore = answer.getScore();
        if (maxScore <= 0) {
            submission.setIsCorrect(false);
            submission.setEarnedScore(0);
            submission.setFeedback("배점이 0점인 문제입니다.");
            return;
        }

        try {
            if (ollamaClient.isAvailable()) {
                gradeWithLlm(submission, answer);
                return;
            }
        } catch (Exception e) {
            log.warn("LLM grading failed, falling back to simple comparison: {}", e.getMessage());
        }

        gradeFallback(submission, answer);
    }

    private void gradeWithLlm(Submission submission, Answer answer) {
        String problemContent = submission.getProblem() != null ? submission.getProblem().getContent() : "";
        int maxScore = answer.getScore();
        String answerContent = answer.getContent() != null ? answer.getContent() : "";
        String submittedAnswer = submission.getSubmittedAnswer() != null ? submission.getSubmittedAnswer() : "";

        String userPrompt = String.format("""
                [문제]
                %s

                [채점 기준] (배점: %d점)
                %s

                [수험자 답안]
                %s

                위 답안을 채점 기준에 따라 채점하고 JSON으로 응답하세요.""",
                problemContent, maxScore, answerContent, submittedAnswer);

        JsonNode result = ollamaClient.chat(SYSTEM_PROMPT, userPrompt);

        if (result != null && result.has("earnedScore")
                && result.get("earnedScore").isNumber()) {
            int earnedScore = result.get("earnedScore").asInt();
            earnedScore = Math.max(0, Math.min(earnedScore, maxScore));
            String feedback = result.has("feedback") ? result.get("feedback").asText() : "";

            submission.setEarnedScore(earnedScore);
            submission.setIsCorrect(earnedScore == maxScore);
            submission.setFeedback(feedback);

            int problemNumber = submission.getProblem() != null ? submission.getProblem().getProblemNumber() : 0;
            log.info("LLM grading - Problem {}: {}/{} - {}",
                    problemNumber, earnedScore, maxScore, feedback);
        } else {
            log.warn("LLM returned invalid or non-numeric earnedScore: {}", result);
            gradeFallback(submission, answer);
        }
    }

    private void gradeFallback(Submission submission, Answer answer) {
        String submittedAnswer = submission.getSubmittedAnswer() != null ? submission.getSubmittedAnswer().trim() : "";
        String answerContent = answer.getContent() != null ? answer.getContent().trim() : "";
        boolean correct = submittedAnswer.equalsIgnoreCase(answerContent);
        submission.setIsCorrect(correct);
        submission.setEarnedScore(correct ? answer.getScore() : 0);
        submission.setFeedback(correct ? "정답" : "오답 (단순 비교 채점)");
    }
}
