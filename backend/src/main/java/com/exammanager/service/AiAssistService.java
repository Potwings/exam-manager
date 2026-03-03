package com.exammanager.service;

import com.exammanager.dto.AiAssistRequest;
import com.exammanager.dto.AiAssistResponse;
import com.exammanager.dto.ChatMessage;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

    /** 대화 이력에서 허용하는 역할. system 역할 주입을 차단하기 위해 명시적으로 제한한다. */
    private static final Set<String> ALLOWED_ROLES = Set.of("user", "assistant");

    public AiAssistResponse generate(AiAssistRequest request) {
        if (!llmClient.isAvailable()) {
            throw new IllegalStateException("AI 서비스를 사용할 수 없습니다.");
        }

        List<ChatMessage> messages = buildMessages(request);
        log.info("AI Assist request: instruction={}, historySize={}", request.getInstruction(),
                request.getConversationHistory() != null ? request.getConversationHistory().size() : 0);

        JsonNode result = llmClient.chat(messages);
        if (result == null) {
            throw new RuntimeException("AI 응답을 받지 못했습니다. 다시 시도해주세요.");
        }

        return parseResponse(result, request);
    }

    /**
     * 멀티턴 메시지 리스트를 조립한다.
     * 순서: system → (필터링된) conversationHistory → 현재 user 메시지
     *
     * system 메시지를 맨 앞에 배치하여 LLM이 전체 대화의 역할과 출력 형식을 인지하게 하고,
     * 이전 대화 이력을 중간에 넣어 맥락을 제공하며,
     * 현재 지시를 맨 마지막에 배치하여 가장 강하게 따르도록 한다.
     */
    List<ChatMessage> buildMessages(AiAssistRequest request) {
        List<ChatMessage> messages = new ArrayList<>();

        // 1) system 메시지 — 역할 정의 및 출력 형식 지정
        messages.add(new ChatMessage("system", SYSTEM_PROMPT));

        // 2) conversationHistory — 이전 대화 맥락 (role 필터링으로 system 주입 차단)
        if (request.getConversationHistory() != null) {
            for (ChatMessage msg : request.getConversationHistory()) {
                if (msg.getRole() != null && ALLOWED_ROLES.contains(msg.getRole())
                        && msg.getContent() != null && !msg.getContent().isBlank()) {
                    messages.add(msg);
                } else {
                    log.warn("대화 이력 메시지 필터링: role={}, hasContent={}", msg.getRole(), msg.getContent() != null);
                }
            }
        }

        // 3) 현재 턴의 user 메시지 — parentContent, contentType, score, instruction 포함
        messages.add(new ChatMessage("user", buildCurrentUserMessage(request)));

        return messages;
    }

    /**
     * 현재 턴의 user 메시지를 구성한다.
     * parentContent(그룹 문제 공통 지문), 메타정보(contentType, score), 관리자 지시를 포함한다.
     */
    String buildCurrentUserMessage(AiAssistRequest request) {
        StringBuilder sb = new StringBuilder();

        // 그룹 문제의 부모 공통 지문이 있으면 [보기] 태그로 맨 앞에 배치 — GradingService와 동일 패턴
        if (hasContent(request.getParentContent())) {
            sb.append("[보기]\n").append(request.getParentContent()).append("\n\n");
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
