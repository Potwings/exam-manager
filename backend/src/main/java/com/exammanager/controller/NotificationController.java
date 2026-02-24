package com.exammanager.controller;

import com.exammanager.dto.AdminCallRequest;
import com.exammanager.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return notificationService.createEmitter();
    }

    @PostMapping("/call-admin")
    public void callAdmin(@Valid @RequestBody AdminCallRequest request) {
        notificationService.notifyAdminCall(
                request.getExamineeId(),
                request.getExamId(),
                request.getExamineeName()
        );
    }
}
