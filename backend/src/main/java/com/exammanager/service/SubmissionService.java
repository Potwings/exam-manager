package com.exammanager.service;

import com.exammanager.dto.SubmissionRequest;
import com.exammanager.dto.SubmissionUpdateRequest;
import com.exammanager.dto.SubmissionResultResponse;
import com.exammanager.dto.ScoreSummaryResponse;
import com.exammanager.entity.*;
import com.exammanager.repository.ExamineeRepository;
import com.exammanager.repository.ProblemRepository;
import com.exammanager.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final ProblemRepository problemRepository;
    private final ExamineeRepository examineeRepository;
    private final GradingService gradingService;
    private final ExamService examService;

    public List<Submission> findByExaminee(Long examineeId) {
        return submissionRepository.findByExamineeId(examineeId);
    }

    public List<Submission> findByExam(Long examId) {
        return submissionRepository.findByProblemExamId(examId);
    }

    @Transactional
    public SubmissionResultResponse submitAnswers(SubmissionRequest request) {
        examService.findById(request.getExamId());

        // 재시험 방지: 이미 해당 시험에 제출한 기록이 있으면 409 반환
        if (submissionRepository.existsByExamineeIdAndProblemExamId(request.getExamineeId(), request.getExamId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 응시 완료한 시험입니다");
        }

        Examinee examinee = examineeRepository.findById(request.getExamineeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "시험자를 찾을 수 없습니다: " + request.getExamineeId()));

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

            submission.setSubmittedAnswer(item.getAnswer());

            if (problem.getAnswer() != null) {
                gradingService.grade(submission, problem.getAnswer());
            }

            submissionRepository.save(submission);
        }

        List<Submission> allSubmissions = submissionRepository
                .findByExamineeIdAndProblemExamId(examinee.getId(), request.getExamId());

        return buildResult(examinee, request.getExamId(), problems, allSubmissions);
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "시험자를 찾을 수 없습니다: " + examineeId));

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
                    return ScoreSummaryResponse.builder()
                            .examineeId(ex.getId())
                            .examineeName(ex.getName())
                            .examineeBirthDate(ex.getBirthDate())
                            .totalScore(total)
                            .maxScore(maxScore)
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
                        .annotatedAnswer(s.getAnnotatedAnswer())
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
