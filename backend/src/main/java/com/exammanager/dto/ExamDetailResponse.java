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
    private Integer timeLimit;
    private boolean hasSubmissions;
    private LocalDateTime createdAt;

    public static ExamDetailResponse from(Exam exam) {
        return buildResponse(exam, false, false);
    }

    public static ExamDetailResponse from(Exam exam, boolean hasSubmissions) {
        return buildResponse(exam, hasSubmissions, true);
    }

    private static ExamDetailResponse buildResponse(Exam exam, boolean hasSubmissions, boolean includeAnswer) {
        // 최상위 문제만 매핑 (자식은 ProblemResponse.children으로 재귀 포함)
        List<ProblemResponse> problemList = exam.getProblems().stream()
                .filter(p -> p.getParent() == null)
                .map(p -> ProblemResponse.from(p, includeAnswer))
                .toList();
        // totalScore: 전체 문제(자식 포함) 중 answer가 있는 것만 합산
        int total = exam.getProblems().stream()
                .filter(p -> p.getAnswer() != null)
                .mapToInt(p -> p.getAnswer().getScore())
                .sum();
        return ExamDetailResponse.builder()
                .id(exam.getId())
                .title(exam.getTitle())
                .problems(problemList)
                .totalScore(total)
                .timeLimit(exam.getTimeLimit())
                .hasSubmissions(hasSubmissions)
                .createdAt(exam.getCreatedAt())
                .build();
    }
}
