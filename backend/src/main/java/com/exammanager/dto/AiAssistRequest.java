package com.exammanager.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class AiAssistRequest {
    @NotBlank
    private String instruction;
    private String parentContent;
    private String contentType = "TEXT";
    private int score = 5;
    private List<ChatMessage> conversationHistory;
}
