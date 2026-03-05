package com.exammanager.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExamSessionMonitorResponse {
    private Long examineeId;
    private String examineeName;
    private LocalDate examineeBirthDate;
    private String status;
    private Long remainingSeconds;
    private LocalDateTime startedAt;
}
