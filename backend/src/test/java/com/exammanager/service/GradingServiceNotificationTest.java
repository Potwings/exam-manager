package com.exammanager.service;

import com.exammanager.entity.Answer;
import com.exammanager.entity.Examinee;
import com.exammanager.entity.Problem;
import com.exammanager.entity.Submission;
import com.exammanager.repository.ExamineeRepository;
import com.exammanager.repository.SubmissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GradingServiceNotificationTest {

    @Mock
    private OllamaClient ollamaClient;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ExamineeRepository examineeRepository;

    @InjectMocks
    private GradingService gradingService;

    @Test
    void gradeSubmissionsAsync_채점_완료_후_알림을_전송한다() {
        // given
        Answer answer = Answer.builder().content("정답").score(10).build();
        Problem problem = Problem.builder().id(1L).problemNumber(1).content("문제").build();
        problem.setAnswer(answer);

        Submission submission = Submission.builder()
                .id(1L)
                .submittedAnswer("정답")
                .problem(problem)
                .build();

        when(submissionRepository.findByExamineeIdAndProblemExamId(1L, 1L))
                .thenReturn(List.of(submission));
        when(ollamaClient.isAvailable()).thenReturn(false); // 폴백 채점 사용
        when(examineeRepository.findById(1L))
                .thenReturn(Optional.of(Examinee.builder().id(1L).name("홍길동").build()));

        // when
        gradingService.gradeSubmissionsAsync(1L, 1L);

        // then
        verify(notificationService).notifyGradingComplete(eq(1L), eq(1L), eq("홍길동"), eq(10), eq(10));
    }

    @Test
    void gradeSubmissionsAsync_수험자_조회_실패해도_채점은_완료된다() {
        // given
        Answer answer = Answer.builder().content("정답").score(5).build();
        Problem problem = Problem.builder().id(1L).problemNumber(1).content("문제").build();
        problem.setAnswer(answer);

        Submission submission = Submission.builder()
                .id(1L)
                .submittedAnswer("정답")
                .problem(problem)
                .build();

        when(submissionRepository.findByExamineeIdAndProblemExamId(1L, 1L))
                .thenReturn(List.of(submission));
        when(ollamaClient.isAvailable()).thenReturn(false);
        when(examineeRepository.findById(1L)).thenReturn(Optional.empty());

        // when
        gradingService.gradeSubmissionsAsync(1L, 1L);

        // then — 수험자 이름은 "알 수 없음"으로 전달
        verify(notificationService).notifyGradingComplete(eq(1L), eq(1L), eq("알 수 없음"), eq(5), eq(5));
    }

    @Test
    void gradeSubmissionsAsync_알림_전송_실패해도_예외가_전파되지_않는다() {
        // given
        Answer answer = Answer.builder().content("정답").score(10).build();
        Problem problem = Problem.builder().id(1L).problemNumber(1).content("문제").build();
        problem.setAnswer(answer);

        Submission submission = Submission.builder()
                .id(1L)
                .submittedAnswer("정답")
                .problem(problem)
                .build();

        when(submissionRepository.findByExamineeIdAndProblemExamId(1L, 1L))
                .thenReturn(List.of(submission));
        when(ollamaClient.isAvailable()).thenReturn(false);
        when(examineeRepository.findById(1L))
                .thenReturn(Optional.of(Examinee.builder().id(1L).name("홍길동").build()));
        doThrow(new RuntimeException("SSE 전송 실패"))
                .when(notificationService).notifyGradingComplete(anyLong(), anyLong(), anyString(), anyInt(), anyInt());

        // when — 예외 없이 정상 종료되어야 함
        gradingService.gradeSubmissionsAsync(1L, 1L);

        // then — 알림 호출은 시도됨
        verify(notificationService).notifyGradingComplete(anyLong(), anyLong(), anyString(), anyInt(), anyInt());
    }
}
