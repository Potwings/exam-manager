package com.exammanager.service;

import com.exammanager.dto.ExamCreateRequest;
import com.exammanager.entity.Answer;
import com.exammanager.entity.Exam;
import com.exammanager.entity.Problem;
import com.exammanager.repository.ExamRepository;
import com.exammanager.repository.ProblemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamRepository examRepository;
    private final ProblemRepository problemRepository;
    private final DocxParserService docxParserService;

    @Transactional
    public Exam createExam(ExamCreateRequest request) {
        Exam exam = Exam.builder()
                .title(request.getTitle())
                .build();

        List<ExamCreateRequest.ProblemInput> problemInputs = Optional.ofNullable(request.getProblems())
                .orElse(Collections.emptyList());

        for (ExamCreateRequest.ProblemInput pi : problemInputs) {
            Problem problem = Problem.builder()
                    .problemNumber(pi.getProblemNumber())
                    .content(pi.getContent())
                    .contentType(pi.getContentType() != null ? pi.getContentType() : "TEXT")
                    .exam(exam)
                    .build();
            Answer answer = Answer.builder()
                    .content(pi.getAnswerContent())
                    .score(pi.getScore())
                    .problem(problem)
                    .build();
            problem.setAnswer(answer);
            exam.getProblems().add(problem);
        }

        return examRepository.save(exam);
    }

    @Transactional
    public Exam createExamFromDocx(String title, MultipartFile problemFile, MultipartFile answerFile) {
        List<Map<String, String>> parsedProblems = docxParserService.parseProblems(problemFile);
        List<Map<String, String>> parsedAnswers = docxParserService.parseAnswers(answerFile);

        Exam exam = Exam.builder()
                .title(title)
                .problemFileName(problemFile.getOriginalFilename())
                .answerFileName(answerFile.getOriginalFilename())
                .build();

        for (Map<String, String> pp : parsedProblems) {
            int num = Integer.parseInt(pp.get("number"));
            String content = pp.get("content");

            Problem problem = Problem.builder()
                    .problemNumber(num)
                    .content(content)
                    .contentType("TEXT")
                    .exam(exam)
                    .build();

            // 문제번호로 매칭되는 답안 찾기
            parsedAnswers.stream()
                    .filter(a -> Integer.parseInt(a.get("number")) == num)
                    .findFirst()
                    .ifPresent(pa -> {
                        Answer answer = Answer.builder()
                                .content(pa.get("content"))
                                .score(Integer.parseInt(pa.get("score")))
                                .problem(problem)
                                .build();
                        problem.setAnswer(answer);
                    });

            exam.getProblems().add(problem);
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
}
