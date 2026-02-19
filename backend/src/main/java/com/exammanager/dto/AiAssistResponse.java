package com.exammanager.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiAssistResponse {
    private String problemContent;
    private String contentType;
    private String answerContent;
    private int score;
}
