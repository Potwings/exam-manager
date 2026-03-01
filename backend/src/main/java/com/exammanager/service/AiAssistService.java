package com.exammanager.service;

import com.exammanager.dto.AiAssistRequest;
import com.exammanager.dto.AiAssistResponse;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAssistService {

    private final LlmClient llmClient;

    private static final String SYSTEM_PROMPT = """
            기술 면접 필기시험 출제 전문가. 규칙:
            1. 관리자의 지시를 무조건 준수한다. 관리자의 요청을 임의로 변경하거나 재해석하지 않는다.
            2. answerContent는 채점 기준(루브릭)이다. 사용자의 질문에 대한 답변을 하지 않는다.
               채점 기준 형식: [필수 키워드] 나열, 만점/부분 점수 조건, 감점 규칙
            반드시 JSON으로만 응답: {"problemContent": "문제", "answerContent": "채점 기준", "score": 배점}
            """;

    public AiAssistResponse generate(AiAssistRequest request) {
        if (!llmClient.isAvailable()) {
            throw new IllegalStateException("AI 서비스를 사용할 수 없습니다.");
        }

        String userPrompt = buildUserPrompt(request);
        log.info("AI Assist request: instruction={}", request.getInstruction());

        JsonNode result = llmClient.chat(SYSTEM_PROMPT, userPrompt);
        if (result == null) {
            throw new RuntimeException("AI 응답을 받지 못했습니다. 다시 시도해주세요.");
        }

        return parseResponse(result, request);
    }

    private String buildUserPrompt(AiAssistRequest request) {
        StringBuilder sb = new StringBuilder();

        // 그룹 문제의 부모 공통 지문이 있으면 [보기] 태그로 맨 앞에 배치 — GradingService와 동일 패턴
        if (hasContent(request.getParentContent())) {
            sb.append("[보기]\n").append(request.getParentContent()).append("\n\n");
        }

        boolean isImprovement = hasContent(request.getCurrentContent()) || hasContent(request.getCurrentAnswer());

        // 기존 콘텐츠가 있으면 참고 자료로 먼저 제시 — 모델이 맥락을 파악한 뒤 지시를 처리하도록 함
        if (isImprovement) {
            if (hasContent(request.getCurrentContent())) {
                sb.append("[현재 문제]\n").append(request.getCurrentContent()).append("\n\n");
            }
            if (hasContent(request.getCurrentAnswer())) {
                sb.append("[현재 채점 기준]\n").append(request.getCurrentAnswer()).append("\n\n");
            }
        }

        sb.append("contentType: ").append(request.getContentType());
        sb.append(", 배점: ").append(request.getScore()).append("점\n\n");

        // 관리자 지시를 맨 마지막에 배치 — LLM이 프롬프트 끝부분을 가장 강하게 따르는 특성 활용
        sb.append("[관리자 지시] ").append(request.getInstruction()).append("\n\n");
        sb.append("관리자의 요청을 무조건 준수하여 JSON으로 응답하세요.");

        return sb.toString();
    }

    private AiAssistResponse parseResponse(JsonNode result, AiAssistRequest request) {
        String problemContent = result.has("problemContent")
                ? result.get("problemContent").asText() : "";
        String answerContent = result.has("answerContent")
                ? result.get("answerContent").asText() : "";
        int score = result.has("score") && result.get("score").isNumber()
                ? result.get("score").asInt() : request.getScore();

        return AiAssistResponse.builder()
                .problemContent(problemContent)
                .contentType(request.getContentType())
                .answerContent(answerContent)
                .score(score)
                .build();
    }

    private boolean hasContent(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
