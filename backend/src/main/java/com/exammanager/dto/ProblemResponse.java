package com.exammanager.dto;

import com.exammanager.entity.Problem;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProblemResponse {
    private Long id;
    private Integer problemNumber;
    private String content;
    private String contentType;
    private String answerContent;
    private Integer score;

    public static ProblemResponse from(Problem problem) {
        return from(problem, false);
    }

    public static ProblemResponse from(Problem problem, boolean includeAnswer) {
        ProblemResponseBuilder builder = ProblemResponse.builder()
                .id(problem.getId())
                .problemNumber(problem.getProblemNumber())
                .content(problem.getContent())
                .contentType(problem.getContentType());

        if (includeAnswer && problem.getAnswer() != null) {
            builder.answerContent(problem.getAnswer().getContent())
                   .score(problem.getAnswer().getScore());
        }

        return builder.build();
    }
}
