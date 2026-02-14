package com.exammanager.controller;

import com.exammanager.dto.ScoreSummaryResponse;
import com.exammanager.service.ExamService;
import com.exammanager.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/scores")
@RequiredArgsConstructor
public class ScoreController {

    private final SubmissionService submissionService;
    private final ExamService examService;

    @GetMapping("/exam/{examId}")
    public ResponseEntity<List<ScoreSummaryResponse>> getScoresByExam(@PathVariable Long examId) {
        examService.findById(examId);
        return ResponseEntity.ok(submissionService.getScoreSummary(examId));
    }
}
