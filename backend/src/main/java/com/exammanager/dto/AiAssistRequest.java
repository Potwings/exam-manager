package com.exammanager.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class AiAssistRequest {
    private String instruction;
    private String currentContent;
    private String currentAnswer;
    private String contentType = "TEXT";
    private int score = 5;
}
