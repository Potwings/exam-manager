package com.exammanager.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ExamSessionRequest {
    @NotNull
    private Long examineeId;
    @NotNull
    private Long examId;
}
