package com.exammanager.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ExamineeLoginRequest {
    @NotBlank(message = "이름은 필수입니다")
    private String name;
}
