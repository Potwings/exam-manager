package com.exammanager.controller;

import com.exammanager.dto.AiAssistRequest;
import com.exammanager.dto.AiAssistResponse;
import com.exammanager.service.AiAssistService;
import com.exammanager.service.OllamaClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai-assist")
@RequiredArgsConstructor
public class AiAssistController {

    private final AiAssistService aiAssistService;
    private final OllamaClient ollamaClient;

    @PostMapping("/generate")
    public ResponseEntity<AiAssistResponse> generate(@RequestBody AiAssistRequest request) {
        AiAssistResponse response = aiAssistService.generate(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> status() {
        boolean available = ollamaClient.isAvailable();
        return ResponseEntity.ok(Map.of("available", available));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleUnavailable(IllegalStateException e) {
        return ResponseEntity.status(503).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleError(RuntimeException e) {
        return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
    }
}
