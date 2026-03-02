package com.exammanager.service;

import com.fasterxml.jackson.databind.JsonNode;

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
     *
     * @param systemPrompt 시스템 역할 프롬프트
     * @param userPrompt   사용자 메시지
     * @return 파싱된 JSON 응답, 실패 시 null
     */
    JsonNode chat(String systemPrompt, String userPrompt);
}
