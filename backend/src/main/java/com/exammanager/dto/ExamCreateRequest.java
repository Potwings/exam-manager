package com.exammanager.dto;

import jakarta.validation.constraints.Min;
import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ExamCreateRequest {
    private String title;
    @Min(1)
    private Integer timeLimit;
    private List<ProblemInput> problems;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class ProblemInput {
        private int problemNumber;
        private String content;
        private String contentType = "TEXT";
        private String answerContent;
        private int score;
    }
}
