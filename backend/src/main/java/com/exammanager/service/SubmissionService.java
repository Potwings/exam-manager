package com.exammanager.service;

import com.exammanager.dto.SubmissionRequest;
import com.exammanager.dto.SubmissionUpdateRequest;
import com.exammanager.dto.SubmissionResultResponse;
import com.exammanager.dto.ScoreSummaryResponse;
import com.exammanager.entity.*;
import com.exammanager.repository.ExamSessionRepository;
import com.exammanager.repository.ExamineeRepository;
import com.exammanager.repository.ProblemRepository;
import com.exammanager.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final ProblemRepository problemRepository;
    private final ExamineeRepository examineeRepository;
    private final ExamSessionRepository examSessionRepository;
    private final GradingService gradingService;
    private final ExamService examService;

    public List<Submission> findByExaminee(Long examineeId) {
        return submissionRepository.findByExamineeId(examineeId);
    }

    public List<Submission> findByExam(Long examId) {
        return submissionRepository.findByProblemExamId(examId);
    }

    @Transactional
    public void submitAnswers(SubmissionRequest request) {
        Exam exam = examService.findById(request.getExamId());

        // 재시험 방지: 이미 해당 시험에 제출한 기록이 있으면 409 반환
        if (submissionRepository.existsByExamineeIdAndProblemExamId(request.getExamineeId(), request.getExamId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 응시 완료한 시험입니다");
        }

        Examinee examinee = examineeRepository.findById(request.getExamineeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "시험자를 찾을 수 없습니다"));

        // 시간 제한 검증: timeLimit이 설정된 시험은 시간 초과 여부 확인 (1분 여유시간 부여)
        if (exam.getTimeLimit() != null) {
            ExamSession session = examSessionRepository.findByExamineeIdAndExamId(request.getExamineeId(), request.getExamId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "시험 시간이 종료되었습니다"));
            LocalDateTime deadline = session.getStartedAt()
                    .plusMinutes(exam.getTimeLimit())
                    .plusMinutes(1);
            if (LocalDateTime.now().isAfter(deadline)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "시험 시간이 종료되었습니다");
            }
        }

        List<Problem> problems = problemRepository.findByExamIdOrderByProblemNumber(request.getExamId());
        Map<Long, Problem> problemMap = problems.stream()
                .collect(Collectors.toMap(Problem::getId, p -> p));

        List<SubmissionRequest.AnswerItem> answers = Optional.ofNullable(request.getAnswers())
                .orElse(Collections.emptyList());

        // problemId 기준 중복 제거 (마지막 항목 유지)
        Map<Long, SubmissionRequest.AnswerItem> deduplicated = new LinkedHashMap<>();
        for (SubmissionRequest.AnswerItem item : answers) {
            deduplicated.put(item.getProblemId(), item);
        }

        for (SubmissionRequest.AnswerItem item : deduplicated.values()) {
            Problem problem = problemMap.get(item.getProblemId());
            if (problem == null) continue;

            Submission submission = submissionRepository
                    .findByExamineeIdAndProblemId(examinee.getId(), problem.getId())
                    .orElse(Submission.builder()
                            .examinee(examinee)
                            .problem(problem)
                            .build());

            // 답안만 저장 (채점은 비동기로 별도 처리)
            submission.setSubmittedAnswer(item.getAnswer());
            submissionRepository.save(submission);
        }

        // 트랜잭션 커밋이 완료된 후에 비동기 채점을 트리거
        // (커밋 전에 호출하면 비동기 스레드에서 아직 저장 안 된 데이터를 조회할 수 없음)
        Long examineeId = examinee.getId();
        Long examId = request.getExamId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                gradingService.gradeSubmissionsAsync(examineeId, examId);
            }
        });
    }

    @Transactional
    public Submission updateSubmission(Long id, SubmissionUpdateRequest request) {
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "제출 답안을 찾을 수 없습니다"));

        int maxScore = submission.getProblem().getAnswer() != null
                ? submission.getProblem().getAnswer().getScore() : 0;
        if (request.getEarnedScore() > maxScore) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "득점이 배점을 초과할 수 없습니다");
        }

        submission.setEarnedScore(request.getEarnedScore());
        submission.setFeedback(request.getFeedback());
        return submissionRepository.save(submission);
    }

    public SubmissionResultResponse getResult(Long examineeId, Long examId) {
        examService.findById(examId);

        Examinee examinee = examineeRepository.findById(examineeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "시험자를 찾을 수 없습니다"));

        List<Problem> problems = problemRepository.findByExamIdOrderByProblemNumber(examId);
        List<Submission> submissions = submissionRepository.findByExamineeIdAndProblemExamId(examineeId, examId);

        return buildResult(examinee, examId, problems, submissions);
    }

    public List<ScoreSummaryResponse> getScoreSummary(Long examId) {
        examService.findById(examId);

        List<Submission> allSubmissions = submissionRepository.findByProblemExamId(examId);

        int maxScore = problemRepository.findByExamIdOrderByProblemNumber(examId).stream()
                .filter(p -> p.getAnswer() != null)
                .mapToInt(p -> p.getAnswer().getScore())
                .sum();

        Map<Long, List<Submission>> byExaminee = allSubmissions.stream()
                .collect(Collectors.groupingBy(s -> s.getExaminee().getId()));

        return byExaminee.entrySet().stream()
                .map(entry -> {
                    List<Submission> subs = entry.getValue();
                    Examinee ex = subs.get(0).getExaminee();
                    int total = subs.stream().mapToInt(s -> s.getEarnedScore() != null ? s.getEarnedScore() : 0).sum();
                    boolean allGraded = subs.stream().allMatch(s -> s.getEarnedScore() != null);
                    return ScoreSummaryResponse.builder()
                            .examineeId(ex.getId())
                            .examineeName(ex.getName())
                            .examineeBirthDate(ex.getBirthDate())
                            .totalScore(total)
                            .maxScore(maxScore)
                            .gradingComplete(allGraded)
                            .submittedAt(subs.stream()
                                    .map(Submission::getSubmittedAt)
                                    .filter(Objects::nonNull)
                                    .max(java.time.LocalDateTime::compareTo)
                                    .orElse(null))
                            .build();
                })
                .sorted(Comparator.comparingInt(ScoreSummaryResponse::getTotalScore).reversed())
                .toList();
    }

    private SubmissionResultResponse buildResult(Examinee examinee, Long examId,
                                                  List<Problem> problems, List<Submission> submissions) {
        int maxScore = problems.stream()
                .filter(p -> p.getAnswer() != null)
                .mapToInt(p -> p.getAnswer().getScore())
                .sum();

        int totalScore = submissions.stream()
                .mapToInt(s -> s.getEarnedScore() != null ? s.getEarnedScore() : 0)
                .sum();

        List<SubmissionResultResponse.SubmissionDetail> details = submissions.stream()
                .map(s -> SubmissionResultResponse.SubmissionDetail.builder()
                        .id(s.getId())
                        .problemNumber(s.getProblem().getProblemNumber())
                        .submittedAnswer(s.getSubmittedAnswer())
                        .isCorrect(s.getIsCorrect())
                        .earnedScore(s.getEarnedScore())
                        .maxScore(s.getProblem().getAnswer() != null ? s.getProblem().getAnswer().getScore() : 0)
                        .feedback(s.getFeedback())
                        .build())
                .sorted(Comparator.comparingInt(SubmissionResultResponse.SubmissionDetail::getProblemNumber))
                .toList();

        Exam exam = problems.isEmpty() ? null : problems.get(0).getExam();

        return SubmissionResultResponse.builder()
                .examineeId(examinee.getId())
                .examineeName(examinee.getName())
                .examId(examId)
                .examTitle(exam != null ? exam.getTitle() : "")
                .totalScore(totalScore)
                .maxScore(maxScore)
                .submissions(details)
                .build();
    }
}
