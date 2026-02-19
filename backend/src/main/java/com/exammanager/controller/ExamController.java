package com.exammanager.controller;

import com.exammanager.dto.ExamCreateRequest;
import com.exammanager.dto.ExamDetailResponse;
import com.exammanager.dto.ExamResponse;
import com.exammanager.dto.ProblemResponse;
import com.exammanager.entity.Exam;
import com.exammanager.service.ExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    @GetMapping
    public ResponseEntity<List<ExamResponse>> list() {
        List<ExamResponse> response = examService.findAll().stream()
                .map(ExamResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExamDetailResponse> get(@PathVariable Long id) {
        Exam exam = examService.findById(id);
        boolean hasSubs = examService.hasSubmissions(id);
        return ResponseEntity.ok(ExamDetailResponse.from(exam, hasSubs));
    }

    @GetMapping("/{id}/problems")
    public ResponseEntity<List<ProblemResponse>> getProblems(@PathVariable Long id) {
        List<ProblemResponse> problems = examService.findProblemsByExamId(id).stream()
                .map(ProblemResponse::from)
                .toList();
        return ResponseEntity.ok(problems);
    }

    @PostMapping
    public ResponseEntity<ExamResponse> create(@RequestBody ExamCreateRequest request) {
        Exam exam = examService.createExam(request);
        return ResponseEntity.ok(ExamResponse.from(exam));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ExamResponse> createFromDocx(
            @RequestParam String title,
            @RequestParam MultipartFile problemFile,
            @RequestParam MultipartFile answerFile) {
        Exam exam = examService.createExamFromDocx(title, problemFile, answerFile);
        return ResponseEntity.ok(ExamResponse.from(exam));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExamResponse> update(@PathVariable Long id, @RequestBody ExamCreateRequest request) {
        Exam exam = examService.updateExam(id, request);
        return ResponseEntity.ok(ExamResponse.from(exam));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        examService.deleteExam(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activate(@PathVariable Long id) {
        examService.activateExam(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/active")
    public ResponseEntity<ExamDetailResponse> getActive() {
        return examService.findActiveExam()
                .map(exam -> ResponseEntity.ok(ExamDetailResponse.from(exam)))
                .orElse(ResponseEntity.noContent().build());
    }
}
