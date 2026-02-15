package com.exammanager.dto;

import com.exammanager.entity.Exam;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExamDetailResponse {
    private Long id;
    private String title;
    private List<ProblemResponse> problems;
    private int totalScore;
    private LocalDateTime createdAt;

    public static ExamDetailResponse from(Exam exam) {
        List<ProblemResponse> problemList = exam.getProblems().stream()
                .map(ProblemResponse::from)
                .toList();
        int total = exam.getProblems().stream()
                .filter(p -> p.getAnswer() != null)
                .mapToInt(p -> p.getAnswer().getScore())
                .sum();
        return ExamDetailResponse.builder()
                .id(exam.getId())
                .title(exam.getTitle())
                .problems(problemList)
                .totalScore(total)
                .createdAt(exam.getCreatedAt())
                .build();
    }
}
