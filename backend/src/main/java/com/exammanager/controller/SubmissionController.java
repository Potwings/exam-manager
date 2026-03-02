package com.exammanager.controller;

import com.exammanager.dto.RegradeRequest;
import com.exammanager.dto.SubmissionRequest;
import com.exammanager.dto.SubmissionUpdateRequest;
import com.exammanager.dto.SubmissionResultResponse;
import com.exammanager.service.SubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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

    @PatchMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                     @Valid @RequestBody SubmissionUpdateRequest request) {
        var submission = submissionService.updateSubmission(id, request);
        Map<String, Object> response = new HashMap<>();
        response.put("id", submission.getId());
        response.put("earnedScore", submission.getEarnedScore());
        response.put("feedback", submission.getFeedback());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/regrade")
    public ResponseEntity<?> regrade(@PathVariable Long id) {
        submissionService.regradeSubmission(id);
        return ResponseEntity.ok(Map.of("message", "재채점이 시작되었습니다"));
    }

    @PostMapping("/regrade")
    public ResponseEntity<?> regradeAll(@Valid @RequestBody RegradeRequest request) {
        int count = submissionService.regradeAllSubmissions(request.getExamineeId(), request.getExamId());
        Map<String, Object> response = new HashMap<>();
        response.put("message", "재채점이 시작되었습니다");
        response.put("count", count);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/result")
    public ResponseEntity<SubmissionResultResponse> getResult(
            @RequestParam Long examineeId,
            @RequestParam Long examId) {
        return ResponseEntity.ok(submissionService.getResult(examineeId, examId));
    }
}
