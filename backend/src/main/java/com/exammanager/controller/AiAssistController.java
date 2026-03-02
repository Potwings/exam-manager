package com.exammanager.controller;

import com.exammanager.dto.AiAssistRequest;
import com.exammanager.dto.AiAssistResponse;
import com.exammanager.service.AiAssistService;
import com.exammanager.service.LlmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/ai-assist")
@RequiredArgsConstructor
public class AiAssistController {

    private final AiAssistService aiAssistService;
    private final LlmClient llmClient;

    @PostMapping("/generate")
    public ResponseEntity<AiAssistResponse> generate(@RequestBody AiAssistRequest request) {
        AiAssistResponse response = aiAssistService.generate(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> status() {
        boolean available = llmClient.isAvailable();
        return ResponseEntity.ok(Map.of("available", available));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleUnavailable(IllegalStateException e) {
        log.warn("AI 서비스 비가용: {}", e.getMessage());
        return ResponseEntity.status(503).body(Map.of("error", "AI 서비스를 사용할 수 없습니다"));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleError(RuntimeException e) {
        log.error("AI 출제 도우미 처리 중 오류 발생", e);
        return ResponseEntity.internalServerError().body(Map.of("error", "AI 처리 중 오류가 발생했습니다"));
    }
}
