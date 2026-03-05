package com.exammanager.config;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 시험 시간 계산 유틸리티.
 * MonitorService, ExamSessionController에서 공용으로 사용.
 */
public final class ExamTimeUtils {

    private ExamTimeUtils() {}

    /**
     * startedAt + timeLimit(분) 기준으로 현재 시각까지의 남은 초를 계산한다.
     * 0 미만은 0으로 보정한다.
     */
    public static long calculateRemainingSeconds(LocalDateTime startedAt, int timeLimitMinutes) {
        LocalDateTime endTime = startedAt.plusMinutes(timeLimitMinutes);
        long remaining = Duration.between(LocalDateTime.now(), endTime).getSeconds();
        return Math.max(remaining, 0);
    }
}
