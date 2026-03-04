package com.exammanager.service;

import com.exammanager.dto.ChatMessage;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * LLM 클라이언트 추상화 인터페이스.
 * Ollama/OpenAI 등 다양한 LLM 프로바이더를 교체 가능하게 한다.
 */
public interface LlmClient {

    /**
     * LLM 서비스가 사용 가능한 상태인지 확인한다.
     */
    boolean isAvailable();

    /**
     * 시스템 프롬프트와 유저 프롬프트를 전달하여 JSON 응답을 받는다.
     * GradingService 등 기존 단일 턴 호출 코드와의 하위 호환을 위해 유지한다.
     *
     * @param systemPrompt 시스템 역할 프롬프트
     * @param userPrompt   사용자 메시지
     * @return 파싱된 JSON 응답, 실패 시 null
     */
    JsonNode chat(String systemPrompt, String userPrompt);

    /**
     * 멀티턴 대화 메시지 리스트를 전달하여 JSON 응답을 받는다.
     * AiAssistService의 대화 이력 기반 출제에서 사용한다.
     *
     * @param messages system/user/assistant 역할의 메시지 리스트
     * @return 파싱된 JSON 응답, 실패 시 null
     */
    default JsonNode chat(List<ChatMessage> messages) {
        throw new UnsupportedOperationException("멀티턴 chat은 이 LLM 클라이언트에서 지원되지 않습니다.");
    }
}
