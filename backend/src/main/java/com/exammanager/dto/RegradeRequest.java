package com.exammanager.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RegradeRequest {
    @NotNull
    private Long examineeId;
    @NotNull
    private Long examId;
}
