package com.exammanager.dto;

import lombok.*;

/**
 * LLM 멀티턴 대화의 개별 메시지를 나타내는 DTO.
 * role: "system", "user", "assistant" 중 하나.
 * content: 메시지 내용.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {
    private String role;
    private String content;
}
