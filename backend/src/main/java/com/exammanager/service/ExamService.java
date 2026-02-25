package com.exammanager.service;

import com.exammanager.dto.ExamCreateRequest;
import com.exammanager.dto.ProblemUpdateRequest;
import com.exammanager.entity.Answer;
import com.exammanager.entity.Exam;
import com.exammanager.entity.Problem;
import com.exammanager.repository.ExamRepository;
import com.exammanager.repository.ProblemRepository;
import com.exammanager.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamRepository examRepository;
    private final ProblemRepository problemRepository;
    private final SubmissionRepository submissionRepository;
    @Transactional
    public Exam createExam(ExamCreateRequest request) {
        Exam exam = Exam.builder()
                .title(request.getTitle())
                .timeLimit(request.getTimeLimit())
                .build();

        List<ExamCreateRequest.ProblemInput> problemInputs = Optional.ofNullable(request.getProblems())
                .orElse(Collections.emptyList());

        for (ExamCreateRequest.ProblemInput pi : problemInputs) {
            Problem problem = buildProblem(pi, exam);
            exam.getProblems().add(problem);

            if (pi.getChildren() != null) {
                for (ExamCreateRequest.ProblemInput ci : pi.getChildren()) {
                    Problem child = buildProblem(ci, exam);
                    child.setParent(problem);
                    problem.getChildren().add(child);
                    exam.getProblems().add(child);
                }
            }
        }

        return examRepository.save(exam);
    }

    public List<Exam> findAll() {
        return examRepository.findByDeletedFalse();
    }

    public Exam findById(Long id) {
        return examRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "시험을 찾을 수 없습니다: " + id));
    }

    public List<Problem> findProblemsByExamId(Long examId) {
        findById(examId);
        return problemRepository.findByExamIdOrderByProblemNumber(examId);
    }

    @Transactional
    public void deleteExam(Long id) {
        Exam exam = findById(id);
        exam.setDeleted(true);
        exam.setActive(false);
        examRepository.save(exam);
    }

    @Transactional
    public void activateExam(Long id) {
        Exam exam = findById(id);
        if (Boolean.TRUE.equals(exam.getDeleted())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "삭제된 시험은 활성화할 수 없습니다: " + id);
        }

        examRepository.findByActiveTrueAndDeletedFalse()
                .ifPresent(e -> {
                    e.setActive(false);
                    examRepository.save(e);
                });

        exam.setActive(true);
        examRepository.save(exam);
    }

    public Optional<Exam> findActiveExam() {
        return examRepository.findByActiveTrueAndDeletedFalse();
    }

    @Transactional
    public Exam updateExam(Long id, ExamCreateRequest request) {
        Exam exam = findById(id);

        if (Boolean.TRUE.equals(exam.getDeleted())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "삭제된 시험은 수정할 수 없습니다: " + id);
        }

        if (submissionRepository.existsByProblemExamId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "제출 결과가 있는 시험은 수정할 수 없습니다. 복제하여 새 시험을 만들어주세요.");
        }

        exam.setTitle(request.getTitle());
        exam.setTimeLimit(request.getTimeLimit());

        // 제출 결과가 없으므로 orphanRemoval로 기존 문제/답안 안전 삭제
        exam.getProblems().clear();
        examRepository.flush();

        List<ExamCreateRequest.ProblemInput> problemInputs = Optional.ofNullable(request.getProblems())
                .orElse(Collections.emptyList());

        for (ExamCreateRequest.ProblemInput pi : problemInputs) {
            Problem problem = buildProblem(pi, exam);
            exam.getProblems().add(problem);

            if (pi.getChildren() != null) {
                for (ExamCreateRequest.ProblemInput ci : pi.getChildren()) {
                    Problem child = buildProblem(ci, exam);
                    child.setParent(problem);
                    problem.getChildren().add(child);
                    exam.getProblems().add(child);
                }
            }
        }

        return examRepository.save(exam);
    }

    public boolean hasSubmissions(Long examId) {
        return submissionRepository.existsByProblemExamId(examId);
    }

    @Transactional
    public Problem updateProblem(Long examId, Long problemId, ProblemUpdateRequest request) {
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "문제를 찾을 수 없습니다: " + problemId));

        if (!problem.getExam().getId().equals(examId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "해당 시험에 속한 문제가 아닙니다.");
        }

        if (Boolean.TRUE.equals(problem.getExam().getDeleted())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "삭제된 시험은 수정할 수 없습니다.");
        }

        boolean isGroupParent = problem.getChildren() != null && !problem.getChildren().isEmpty();

        problem.setContent(request.getContent());
        problem.setContentType(request.getContentType() != null ? request.getContentType() : "TEXT");
        problem.setCodeEditor(Boolean.TRUE.equals(request.getCodeEditor()));
        problem.setCodeLanguage(request.getCodeLanguage());

        if (!isGroupParent) {
            if (request.getAnswerContent() == null || request.getAnswerContent().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "채점 기준은 필수입니다.");
            }
            applyAnswer(problem, request.getAnswerContent(), request.getScore());
        }

        return problemRepository.save(problem);
    }

    private void applyAnswer(Problem problem, String answerContent, Integer score) {
        if (answerContent == null || answerContent.isBlank()) return;

        if (score == null || score <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "문제 " + problem.getProblemNumber() + "번의 배점은 1점 이상이어야 합니다.");
        }

        Answer answer = problem.getAnswer();
        if (answer != null) {
            answer.setContent(answerContent);
            answer.setScore(score);
        } else {
            answer = Answer.builder()
                    .content(answerContent)
                    .score(score)
                    .problem(problem)
                    .build();
            problem.setAnswer(answer);
        }
    }

    private Problem buildProblem(ExamCreateRequest.ProblemInput pi, Exam exam) {
        Problem problem = Problem.builder()
                .problemNumber(pi.getProblemNumber())
                .content(pi.getContent())
                .contentType(pi.getContentType() != null ? pi.getContentType() : "TEXT")
                .codeEditor(pi.isCodeEditor())
                .codeLanguage(pi.getCodeLanguage())
                .exam(exam)
                .build();

        // answerContent가 있는 경우에만 Answer 생성 (지문 전용 부모는 스킵)
        applyAnswer(problem, pi.getAnswerContent(), pi.getScore());

        return problem;
    }
}
