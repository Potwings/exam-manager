package com.exammanager.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ProblemUpdateRequest {
    @NotBlank
    private String content;
    private String contentType;    // "TEXT" | "MARKDOWN"
    private Boolean codeEditor;
    private String codeLanguage;
    private String answerContent;  // 그룹 부모는 null 허용
    private Integer score;         // 그룹 부모는 null 허용
}
