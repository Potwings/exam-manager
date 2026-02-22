package com.exammanager.dto;

import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SubmissionResultResponse {
    private Long examineeId;
    private String examineeName;
    private Long examId;
    private String examTitle;
    private int totalScore;
    private int maxScore;
    private List<SubmissionDetail> submissions;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SubmissionDetail {
        private Long id;
        private Integer problemNumber;
        private String submittedAnswer;
        private Boolean isCorrect;
        private Integer earnedScore;
        private Integer maxScore;
        private String feedback;
        private String annotatedAnswer;
    }
}
