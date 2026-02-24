package com.exammanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class AdminCallRequest {
    @NotNull
    private Long examineeId;
    @NotNull
    private Long examId;
    @NotBlank
    private String examineeName;
}
