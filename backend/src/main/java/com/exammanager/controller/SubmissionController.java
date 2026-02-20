package com.exammanager.controller;

import com.exammanager.dto.SubmissionRequest;
import com.exammanager.dto.SubmissionResultResponse;
import com.exammanager.service.SubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    @PostMapping
    public ResponseEntity<?> submit(@Valid @RequestBody SubmissionRequest request) {
        // 채점은 백엔드에서 정상 수행하지만, 응답에서는 점수/피드백을 제외하고 성공 메시지만 반환
        submissionService.submitAnswers(request);
        return ResponseEntity.ok(Map.of("message", "답안이 정상적으로 제출되었습니다"));
    }

    @GetMapping("/result")
    public ResponseEntity<SubmissionResultResponse> getResult(
            @RequestParam Long examineeId,
            @RequestParam Long examId) {
        return ResponseEntity.ok(submissionService.getResult(examineeId, examId));
    }
}
