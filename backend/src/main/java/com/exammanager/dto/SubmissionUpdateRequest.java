package com.exammanager.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class SubmissionUpdateRequest {
    @NotNull(message = "득점은 필수입니다")
    @Min(value = 0, message = "득점은 0 이상이어야 합니다")
    private Integer earnedScore;

    @NotBlank(message = "채점 사유는 필수입니다")
    private String feedback;
}
