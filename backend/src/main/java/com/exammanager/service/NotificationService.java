package com.exammanager.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class NotificationService {

    private static final long EMITTER_TIMEOUT = 30 * 60 * 1000L; // 30분

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT);

        emitters.add(emitter);
        log.info("SSE 연결 생성 - 현재 연결 수: {}", emitters.size());

        emitter.onCompletion(() -> {
            emitters.remove(emitter);
            log.debug("SSE 연결 종료 (completion) - 현재 연결 수: {}", emitters.size());
        });
        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            log.debug("SSE 연결 종료 (timeout) - 현재 연결 수: {}", emitters.size());
        });
        emitter.onError(e -> {
            emitters.remove(emitter);
            log.debug("SSE 연결 종료 (error) - 현재 연결 수: {}", emitters.size());
        });

        return emitter;
    }

    public void notifyAdminCall(Long examineeId, Long examId, String examineeName) {
        if (emitters.isEmpty()) {
            return;
        }

        Map<String, Object> data = Map.of(
                "examineeId", examineeId,
                "examId", examId,
                "examineeName", examineeName
        );

        log.info("관리자 호출 알림 전송 - {} (시험 ID: {}), 대상 연결 수: {}",
                examineeName, examId, emitters.size());

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("admin-call")
                        .data(data));
            } catch (IOException e) {
                emitters.remove(emitter);
                log.debug("SSE 전송 실패, 연결 제거: {}", e.getMessage());
            }
        }
    }

    public void notifyGradingComplete(Long examineeId, Long examId,
                                      String examineeName, int totalScore, int maxScore) {
        if (emitters.isEmpty()) {
            return;
        }

        Map<String, Object> data = Map.of(
                "examineeId", examineeId,
                "examId", examId,
                "examineeName", examineeName,
                "totalScore", totalScore,
                "maxScore", maxScore
        );

        log.info("채점 완료 알림 전송 - {} ({}점/{}점), 대상 연결 수: {}",
                examineeName, totalScore, maxScore, emitters.size());

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("grading-complete")
                        .data(data));
            } catch (IOException e) {
                emitters.remove(emitter);
                log.debug("SSE 전송 실패, 연결 제거: {}", e.getMessage());
            }
        }
    }
}
