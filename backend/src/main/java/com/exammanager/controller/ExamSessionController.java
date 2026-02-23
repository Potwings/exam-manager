package com.exammanager.controller;

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

import java.time.Duration;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/exam-sessions")
@RequiredArgsConstructor
public class ExamSessionController {

    private final ExamSessionRepository examSessionRepository;
    private final ExamineeRepository examineeRepository;
    private final ExamService examService;

    @PostMapping
    public ExamSessionResponse createSession(@RequestBody ExamSessionRequest request) {
        Exam exam = examService.findById(request.getExamId());

        // timeLimit 없으면 세션 미생성, 시간 제한 없음 응답
        if (exam.getTimeLimit() == null) {
            return ExamSessionResponse.builder().remainingSeconds(null).build();
        }

        Examinee examinee = examineeRepository.findById(request.getExamineeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "시험자를 찾을 수 없습니다"));

        // find-or-create 패턴: 기존 세션이 있으면 재사용, 없으면 새로 생성
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

        long remaining = calculateRemainingSeconds(session, exam);
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

        long remaining = calculateRemainingSeconds(session, exam);
        return ExamSessionResponse.builder().remainingSeconds(remaining).build();
    }

    private long calculateRemainingSeconds(ExamSession session, Exam exam) {
        LocalDateTime endTime = session.getStartedAt().plusMinutes(exam.getTimeLimit());
        long remaining = Duration.between(LocalDateTime.now(), endTime).getSeconds();
        return Math.max(remaining, 0);
    }
}
