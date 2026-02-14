package com.exammanager.dto;

import com.exammanager.entity.Exam;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExamResponse {
    private Long id;
    private String title;
    private int problemCount;
    private int totalScore;
    private Boolean active;
    private LocalDateTime createdAt;

    public static ExamResponse from(Exam exam) {
        int total = exam.getProblems().stream()
                .filter(p -> p.getAnswer() != null)
                .mapToInt(p -> p.getAnswer().getScore())
                .sum();
        return ExamResponse.builder()
                .id(exam.getId())
                .title(exam.getTitle())
                .problemCount(exam.getProblems().size())
                .totalScore(total)
                .active(exam.getActive())
                .createdAt(exam.getCreatedAt())
                .build();
    }
}
