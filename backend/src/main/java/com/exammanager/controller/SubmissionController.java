package com.exammanager.controller;

import com.exammanager.dto.SubmissionRequest;
import com.exammanager.dto.SubmissionResultResponse;
import com.exammanager.service.SubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    @PostMapping
    public ResponseEntity<SubmissionResultResponse> submit(@Valid @RequestBody SubmissionRequest request) {
        return ResponseEntity.ok(submissionService.submitAnswers(request));
    }

    @GetMapping("/result")
    public ResponseEntity<SubmissionResultResponse> getResult(
            @RequestParam Long examineeId,
            @RequestParam Long examId) {
        return ResponseEntity.ok(submissionService.getResult(examineeId, examId));
    }
}
