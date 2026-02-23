package com.exammanager.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExamSessionResponse {
    private Long remainingSeconds;
}
