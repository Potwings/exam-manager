package com.exammanager.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ScoreSummaryResponse {
    private Long examineeId;
    private String examineeName;
    private int totalScore;
    private int maxScore;
    private LocalDateTime submittedAt;
}
