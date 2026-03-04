package com.exammanager.service;

import com.exammanager.dto.SubmissionResultResponse;
import com.exammanager.entity.*;
import com.exammanager.repository.ExamSessionRepository;
import com.exammanager.repository.ExamineeRepository;
import com.exammanager.repository.ProblemRepository;
import com.exammanager.repository.SubmissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * SubmissionService.buildResult() 메서드의 매핑 로직을 검증하는 단위 테스트.
 * buildResult는 private이므로 getResult() public 메서드를 통해 간접 테스트한다.
 */
@ExtendWith(MockitoExtension.class)
class SubmissionServiceBuildResultTest {

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private ProblemRepository problemRepository;

    @Mock
    private ExamineeRepository examineeRepository;

    @Mock
    private ExamSessionRepository examSessionRepository;

    @Mock
    private GradingService gradingService;

    @Mock
    private ExamService examService;

    @InjectMocks
    private SubmissionService submissionService;

    /**
     * Answer가 있는 문제의 submission인 경우,
     * answerContent에 Answer.content 값이 정상 매핑되는지 검증한다.
     */
    @Test
    void getResult_Answer가_있으면_answerContent에_채점기준이_매핑된다() {
        // given
        Long examineeId = 1L;
        Long examId = 10L;

        Exam exam = Exam.builder().id(examId).title("테스트 시험").build();
        Examinee examinee = Examinee.builder().id(examineeId).name("홍길동").build();

        Answer answer = Answer.builder()
                .id(100L)
                .content("정답은 OOP의 4대 원칙을 서술해야 합니다")
                .score(10)
                .build();

        Problem problem = Problem.builder()
                .id(1L)
                .problemNumber(1)
                .content("OOP란 무엇인가?")
                .contentType("TEXT")
                .codeEditor(false)
                .exam(exam)
                .build();
        problem.setAnswer(answer);

        Submission submission = Submission.builder()
                .id(1L)
                .examinee(examinee)
                .problem(problem)
                .submittedAnswer("객체지향 프로그래밍입니다")
                .earnedScore(8)
                .feedback("잘 작성했습니다")
                .build();

        when(examService.findById(examId)).thenReturn(exam);
        when(examineeRepository.findById(examineeId)).thenReturn(Optional.of(examinee));
        when(problemRepository.findByExamIdOrderByProblemNumber(examId)).thenReturn(List.of(problem));
        when(submissionRepository.findByExamineeIdAndProblemExamId(examineeId, examId)).thenReturn(List.of(submission));

        // when
        SubmissionResultResponse result = submissionService.getResult(examineeId, examId);

        // then
        assertThat(result.getSubmissions()).hasSize(1);
        SubmissionResultResponse.SubmissionDetail detail = result.getSubmissions().get(0);
        assertThat(detail.getAnswerContent()).isEqualTo("정답은 OOP의 4대 원칙을 서술해야 합니다");
    }

    /**
     * Answer가 null인 문제의 submission인 경우 (예: 부모 문제는 Answer가 없음),
     * answerContent가 null로 매핑되는지 검증한다.
     */
    @Test
    void getResult_Answer가_null이면_answerContent가_null로_매핑된다() {
        // given
        Long examineeId = 1L;
        Long examId = 10L;

        Exam exam = Exam.builder().id(examId).title("테스트 시험").build();
        Examinee examinee = Examinee.builder().id(examineeId).name("홍길동").build();

        // Answer가 없는 문제 (answer 필드가 null)
        Problem problem = Problem.builder()
                .id(2L)
                .problemNumber(2)
                .content("보기를 읽고 답하시오")
                .contentType("TEXT")
                .codeEditor(false)
                .exam(exam)
                .build();
        // answer를 설정하지 않음 -> null

        Submission submission = Submission.builder()
                .id(2L)
                .examinee(examinee)
                .problem(problem)
                .submittedAnswer("답안입니다")
                .earnedScore(0)
                .build();

        when(examService.findById(examId)).thenReturn(exam);
        when(examineeRepository.findById(examineeId)).thenReturn(Optional.of(examinee));
        when(problemRepository.findByExamIdOrderByProblemNumber(examId)).thenReturn(List.of(problem));
        when(submissionRepository.findByExamineeIdAndProblemExamId(examineeId, examId)).thenReturn(List.of(submission));

        // when
        SubmissionResultResponse result = submissionService.getResult(examineeId, examId);

        // then
        assertThat(result.getSubmissions()).hasSize(1);
        SubmissionResultResponse.SubmissionDetail detail = result.getSubmissions().get(0);
        assertThat(detail.getAnswerContent()).isNull();
    }

    /**
     * Answer가 있는 문제와 없는 문제가 혼재된 경우,
     * 각각의 answerContent가 올바르게 매핑되는지 검증한다.
     */
    @Test
    void getResult_Answer_유무가_혼재된_경우_각각_올바르게_매핑된다() {
        // given
        Long examineeId = 1L;
        Long examId = 10L;

        Exam exam = Exam.builder().id(examId).title("테스트 시험").build();
        Examinee examinee = Examinee.builder().id(examineeId).name("홍길동").build();

        // 문제1: Answer 있음
        Answer answer1 = Answer.builder().id(100L).content("TCP 3-way handshake 설명").score(5).build();
        Problem problem1 = Problem.builder()
                .id(1L).problemNumber(1).content("TCP 연결 과정을 설명하시오")
                .contentType("TEXT").codeEditor(false).exam(exam).build();
        problem1.setAnswer(answer1);

        // 문제2: Answer 없음
        Problem problem2 = Problem.builder()
                .id(2L).problemNumber(2).content("다음 코드의 출력을 쓰시오")
                .contentType("MARKDOWN").codeEditor(true).codeLanguage("java").exam(exam).build();

        Submission sub1 = Submission.builder()
                .id(1L).examinee(examinee).problem(problem1)
                .submittedAnswer("SYN, SYN-ACK, ACK").earnedScore(5).build();

        Submission sub2 = Submission.builder()
                .id(2L).examinee(examinee).problem(problem2)
                .submittedAnswer("Hello World").earnedScore(0).build();

        when(examService.findById(examId)).thenReturn(exam);
        when(examineeRepository.findById(examineeId)).thenReturn(Optional.of(examinee));
        when(problemRepository.findByExamIdOrderByProblemNumber(examId)).thenReturn(List.of(problem1, problem2));
        when(submissionRepository.findByExamineeIdAndProblemExamId(examineeId, examId)).thenReturn(List.of(sub1, sub2));

        // when
        SubmissionResultResponse result = submissionService.getResult(examineeId, examId);

        // then
        assertThat(result.getSubmissions()).hasSize(2);

        // 문제번호 기준 정렬되므로 첫 번째가 problem1
        SubmissionResultResponse.SubmissionDetail detail1 = result.getSubmissions().get(0);
        assertThat(detail1.getProblemNumber()).isEqualTo(1);
        assertThat(detail1.getAnswerContent()).isEqualTo("TCP 3-way handshake 설명");

        SubmissionResultResponse.SubmissionDetail detail2 = result.getSubmissions().get(1);
        assertThat(detail2.getProblemNumber()).isEqualTo(2);
        assertThat(detail2.getAnswerContent()).isNull();
    }
}
