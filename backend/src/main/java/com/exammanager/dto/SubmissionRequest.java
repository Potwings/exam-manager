package com.exammanager.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class SubmissionRequest {
    @NotNull(message = "시험자 ID는 필수입니다")
    private Long examineeId;

    @NotNull(message = "시험 ID는 필수입니다")
    private Long examId;

    @Valid
    @NotNull(message = "답안 목록은 필수입니다")
    private List<AnswerItem> answers;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class AnswerItem {
        @NotNull(message = "문제 ID는 필수입니다")
        private Long problemId;
        private String answer;
    }
}
