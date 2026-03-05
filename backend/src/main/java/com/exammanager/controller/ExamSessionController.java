package com.exammanager.controller;

import com.exammanager.config.ExamTimeUtils;
import com.exammanager.dto.ExamSessionRequest;
import com.exammanager.dto.ExamSessionResponse;
import com.exammanager.entity.Exam;
import com.exammanager.entity.ExamSession;
import com.exammanager.entity.Examinee;
import com.exammanager.repository.ExamSessionRepository;
import com.exammanager.repository.ExamineeRepository;
import com.exammanager.service.ExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/exam-sessions")
@RequiredArgsConstructor
public class ExamSessionController {

    private final ExamSessionRepository examSessionRepository;
    private final ExamineeRepository examineeRepository;
    private final ExamService examService;

    @PostMapping
    public ExamSessionResponse createSession(@Valid @RequestBody ExamSessionRequest request) {
        Exam exam = examService.findById(request.getExamId());

        Examinee examinee = examineeRepository.findById(request.getExamineeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "시험자를 찾을 수 없습니다"));

        // 항상 세션 생성 (find-or-create): 시간 제한 유무와 관계없이 모니터링 추적용
        ExamSession session = examSessionRepository.findByExamineeIdAndExamId(
                request.getExamineeId(), request.getExamId()
        ).orElse(null);

        if (session == null) {
            try {
                session = ExamSession.builder()
                        .examinee(examinee)
                        .exam(exam)
                        .build();
                session = examSessionRepository.save(session);
            } catch (DataIntegrityViolationException e) {
                // 동시 요청으로 UniqueConstraint 충돌 시 기존 세션 조회
                session = examSessionRepository.findByExamineeIdAndExamId(
                        request.getExamineeId(), request.getExamId()
                ).orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "세션 생성 실패"));
            }
        }

        // timeLimit 없으면 시간 제한 없음 응답 (세션은 이미 생성됨)
        if (exam.getTimeLimit() == null) {
            return ExamSessionResponse.builder().remainingSeconds(null).build();
        }

        long remaining = ExamTimeUtils.calculateRemainingSeconds(session.getStartedAt(), exam.getTimeLimit());
        return ExamSessionResponse.builder().remainingSeconds(remaining).build();
    }

    @GetMapping("/remaining")
    public ExamSessionResponse getRemaining(
            @RequestParam Long examineeId,
            @RequestParam Long examId
    ) {
        Exam exam = examService.findById(examId);

        if (exam.getTimeLimit() == null) {
            return ExamSessionResponse.builder().remainingSeconds(null).build();
        }

        ExamSession session = examSessionRepository.findByExamineeIdAndExamId(examineeId, examId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "세션을 찾을 수 없습니다"));

        long remaining = ExamTimeUtils.calculateRemainingSeconds(session.getStartedAt(), exam.getTimeLimit());
        return ExamSessionResponse.builder().remainingSeconds(remaining).build();
    }
}
