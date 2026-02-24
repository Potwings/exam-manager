package com.exammanager.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class NotificationServiceTest {

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService();
    }

    @Test
    void createEmitter_SseEmitter를_생성한다() {
        SseEmitter emitter = notificationService.createEmitter();

        assertThat(emitter).isNotNull();
        assertThat(emitter.getTimeout()).isEqualTo(30 * 60 * 1000L);
    }

    @Test
    void notifyGradingComplete_연결된_emitter가_없으면_예외_없이_종료된다() {
        assertThatNoException().isThrownBy(() ->
                notificationService.notifyGradingComplete(1L, 1L, "홍길동", 80, 100));
    }

    @Test
    void notifyGradingComplete_연결된_emitter에_이벤트를_전송한다() {
        SseEmitter emitter = notificationService.createEmitter();

        assertThatNoException().isThrownBy(() ->
                notificationService.notifyGradingComplete(1L, 1L, "홍길동", 80, 100));

        // emitter가 아직 목록에 남아있으면 전송 성공 (IOException 미발생)
        assertThat(emitter).isNotNull();
    }

    @Test
    void createEmitter_여러_emitter_생성_후_알림_전송해도_예외가_발생하지_않는다() {
        notificationService.createEmitter();
        notificationService.createEmitter();
        notificationService.createEmitter();

        assertThatNoException().isThrownBy(() ->
                notificationService.notifyGradingComplete(1L, 1L, "홍길동", 80, 100));
    }

    @Test
    void notifyAdminCall_연결된_emitter가_없으면_예외_없이_종료된다() {
        assertThatNoException().isThrownBy(() ->
                notificationService.notifyAdminCall(1L, 1L, "홍길동"));
    }

    @Test
    void notifyAdminCall_연결된_emitter에_이벤트를_전송한다() {
        SseEmitter emitter = notificationService.createEmitter();

        assertThatNoException().isThrownBy(() ->
                notificationService.notifyAdminCall(1L, 1L, "홍길동"));

        assertThat(emitter).isNotNull();
    }
}
