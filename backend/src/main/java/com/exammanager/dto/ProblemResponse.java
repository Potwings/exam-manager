package com.exammanager.dto;

import com.exammanager.entity.Problem;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProblemResponse {
    private Long id;
    private Integer problemNumber;
    private String content;
    private String contentType;

    public static ProblemResponse from(Problem problem) {
        return ProblemResponse.builder()
                .id(problem.getId())
                .problemNumber(problem.getProblemNumber())
                .content(problem.getContent())
                .contentType(problem.getContentType())
                .build();
    }
}
