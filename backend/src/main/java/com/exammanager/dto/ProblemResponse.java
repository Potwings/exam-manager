package com.exammanager.dto;

import com.exammanager.entity.Problem;
import lombok.*;

import java.util.Collections;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProblemResponse {
    private Long id;
    private Integer problemNumber;
    private String content;
    private String contentType;
    private Boolean codeEditor;
    private String answerContent;
    private Integer score;
    private List<ProblemResponse> children;

    public static ProblemResponse from(Problem problem) {
        return from(problem, false);
    }

    public static ProblemResponse from(Problem problem, boolean includeAnswer) {
        ProblemResponseBuilder builder = ProblemResponse.builder()
                .id(problem.getId())
                .problemNumber(problem.getProblemNumber())
                .content(problem.getContent())
                .contentType(problem.getContentType())
                .codeEditor(Boolean.TRUE.equals(problem.getCodeEditor()));

        if (includeAnswer && problem.getAnswer() != null) {
            builder.answerContent(problem.getAnswer().getContent())
                   .score(problem.getAnswer().getScore());
        }

        List<Problem> kids = problem.getChildren();
        if (kids != null && !kids.isEmpty()) {
            builder.children(kids.stream()
                    .map(c -> ProblemResponse.from(c, includeAnswer))
                    .toList());
        } else {
            builder.children(Collections.emptyList());
        }

        return builder.build();
    }
}
