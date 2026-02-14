package com.exammanager.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class SubmissionRequest {
    @NotNull(message = "시험자 ID는 필수입니다")
    private Long examineeId;

    @NotNull(message = "시험 ID는 필수입니다")
    private Long examId;

    @NotNull(message = "답안 목록은 필수입니다")
    private List<AnswerItem> answers;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class AnswerItem {
        private Long problemId;
        private String answer;
    }
}
