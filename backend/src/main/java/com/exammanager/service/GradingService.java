package com.exammanager.service;

import com.exammanager.entity.Answer;
import com.exammanager.entity.Examinee;
import com.exammanager.entity.Submission;
import com.exammanager.repository.ExamineeRepository;
import com.exammanager.repository.SubmissionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GradingService {

    private final OllamaClient ollamaClient;
    private final SubmissionRepository submissionRepository;
    private final NotificationService notificationService;
    private final ExamineeRepository examineeRepository;

    private static final String SYSTEM_PROMPT = """
        # 역할
        
        당신은 기술 면접 필기시험의 **채점관**입니다. 수험자의 답안을 채점 기준에 따라 공정하게 채점하세요.
        
        ---
        
        ## 채점 원칙
        
        - 채점 기준과 정확히 일치하지 않더라도, **내용이 맞다면 정답으로 인정**합니다.
        - 정확한 표현이 아니더라도 **의미가 동일하거나 올바른 내용**이면 점수를 부여합니다.
        - 채점 기준에 **"설명"이 요구되는 문제**는 설명이 있어야 해당 점수를 부여합니다.
        - 용어나 키워드만 나열하고 **설명이 없으면** 채점 기준에 명시된 부분 점수만 부여하세요.
        - 답안이 **비어있거나 관련 없는 내용**이면 **0점**입니다.
        - **SQL 문제**: 실행 결과가 정확해야 점수를 부여합니다.
        - **코드 문제**: 문법이 완벽하지 않아도 핵심 로직이 맞으면 부분 점수를 부여합니다.
        
        ---
        
        ## 중요 사항
        
        - 채점 기준의 모범 답안은 **참고 사항**일 뿐, 수험자가 다른 방식으로 올바르게 답변했다면 동일하게 점수를 부여하세요.
        - 정확히 같은 단어가 아니어도 의미나 뉘앙스가 유사하면 인정합니다. (예: "클라이언트에 저장" = "브라우저에 저장" = "로컬에 저장")
        - **핵심 개념을 설명하고 있으면** 표현이 달라도 점수를 부여하세요.
        - **핵심 개념 자체가 빠져있으면** 해당 점수를 부여하지 마세요.
        - 채점 기준에 "이름만 나열하면 감점" 또는 "설명 없으면 N점" 같은 조건이 있으면 **반드시** 따르세요.
        - [채점 기준]은 채점 시 중요하게 봐야 할 핵심 개념입니다.
        
        ---
        
        ## annotatedAnswer 작성 규칙
        
        수험자 답안의 **원문을 최대한 유지**하면서 태그로 감싸세요.
        
        ### 태그 종류
        
        | 태그 | 용도 |
        |------|------|
        | [정답]...[/정답] | 채점 기준에 부합하는 정확한 내용 |
        | [오답]...[/오답] | 잘못되거나 부정확한 내용 |
        | [부분]...[/부분] | 방향은 맞지만 설명이 부족하거나 애매한 내용 |
        
        ### 규칙
        
        - 모든 여는 태그에는 **반드시 닫는 태그를 쌍으로** 작성하세요.
        - 답안 전체가 하나의 카테고리면 전체를 하나의 태그로 감싸세요.
        - 태그 사이에는 **공백 한 칸**을 넣어 구분하세요.
        - 태그 안의 원문 줄바꿈은 그대로 유지하세요.
        - **절대 채점 사유나 설명을 annotatedAnswer에 쓰지 마세요. 채점 근거는 반드시 feedback에만 작성하세요.**
        
        ### 올바른 예시
        
        "[정답]캡슐화는 데이터를 보호하는 것이다.[/정답] [오답]다형성은 변수를 여러 개 쓰는 것이다.[/오답] [부분]상속은 물려받는 것이다.[/부분]"
        
        ### 잘못된 예시 (닫는 태그 누락 - 절대 이렇게 작성하지 마세요)
        
        "[정답]캡슐화는 데이터를 보호하는 것이다. [오답]다형성은 변수를 여러 개 쓰는 것이다."
        
        ---

        ## feedback 작성 규칙

        - **1~2문장**으로 간결하게 작성하세요.
        - 어떤 핵심 개념을 맞혔고 어떤 개념이 부족한지 구체적으로 언급하세요.
        - 만점: 맞힌 핵심 개념을 간단히 언급. (예: "쿠키와 세션의 차이점을 정확히 설명함.")
        - 부분 점수: 맞힌 부분과 부족한 부분을 함께 언급. (예: "쿠키 설명은 정확하나, 세션의 서버 저장 특성 설명이 부족함.")
        - 0점: 오답 사유를 간단히 언급. (예: "질문과 관련 없는 내용.")

        ---

        ## 응답 형식
        
        반드시 아래 JSON 형식으로만 응답하세요. JSON 외의 텍스트를 절대 포함하지 마세요.

        {"earnedScore": 점수, "annotatedAnswer": "태그가 적용된 답안", "feedback": "채점 근거"}
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

    @Async
    @Transactional
    public void gradeSubmissionsAsync(Long examineeId, Long examId) {
        List<Submission> submissions = submissionRepository
                .findByExamineeIdAndProblemExamId(examineeId, examId);

        log.info("비동기 채점 시작 - examineeId: {}, examId: {}, 문제 수: {}",
                examineeId, examId, submissions.size());

        for (Submission submission : submissions) {
            try {
                Answer answer = submission.getProblem().getAnswer();
                if (answer != null) {
                    grade(submission, answer);
                    submissionRepository.save(submission);
                }
            } catch (Exception e) {
                log.error("채점 실패 - submissionId: {}, error: {}",
                        submission.getId(), e.getMessage());
            }
        }

        log.info("비동기 채점 완료 - examineeId: {}, examId: {}", examineeId, examId);

        // 채점 완료 알림 전송
        try {
            int totalScore = submissions.stream()
                    .filter(s -> s.getEarnedScore() != null)
                    .mapToInt(Submission::getEarnedScore)
                    .sum();
            int maxScore = submissions.stream()
                    .filter(s -> s.getProblem() != null && s.getProblem().getAnswer() != null)
                    .mapToInt(s -> s.getProblem().getAnswer().getScore())
                    .sum();
            String examineeName = examineeRepository.findById(examineeId)
                    .map(Examinee::getName)
                    .orElse("알 수 없음");

            notificationService.notifyGradingComplete(examineeId, examId, examineeName, totalScore, maxScore);
        } catch (Exception e) {
            log.warn("채점 완료 알림 전송 실패: {}", e.getMessage());
        }
    }

    private void gradeWithLlm(Submission submission, Answer answer) {
        String problemContent = submission.getProblem() != null ? submission.getProblem().getContent() : "";
        // 자식 문제인 경우 부모 지문을 앞에 포함
        if (submission.getProblem() != null && submission.getProblem().getParent() != null) {
            String parentContent = submission.getProblem().getParent().getContent();
            if (parentContent != null && !parentContent.isBlank()) {
                problemContent = "[보기]\n" + parentContent + "\n\n[문제]\n" + problemContent;
            }
        }
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
            String annotated = result.has("annotatedAnswer") ? result.get("annotatedAnswer").asText() : null;

            submission.setEarnedScore(earnedScore);
            submission.setIsCorrect(earnedScore == maxScore);
            submission.setFeedback(feedback);
            submission.setAnnotatedAnswer(annotated);

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
        submission.setAnnotatedAnswer(correct
                ? "[정답]" + submittedAnswer + "[/정답]"
                : "[오답]" + submittedAnswer + "[/오답]");
    }
}
