package com.exammanager.service;

import com.exammanager.entity.*;
import com.exammanager.repository.ExamSessionRepository;
import com.exammanager.repository.ExamineeRepository;
import com.exammanager.repository.ProblemRepository;
import com.exammanager.repository.SubmissionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceRegradeTest {

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

    @BeforeEach
    void setUp() {
        // TransactionSynchronizationManager를 수동으로 초기화하여
        // afterCommit 콜백 등록이 가능하도록 트랜잭션 동기화 활성화
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        // 테스트 후 동기화 상태를 정리하여 다른 테스트에 영향을 주지 않도록 처리
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    // --- 개별 재채점 ---

    @Test
    void regradeSubmission_정상_재채점_시_regrading이_true로_설정된다() {
        // given
        Submission submission = Submission.builder()
                .id(1L)
                .submittedAnswer("답안")
                .build();
        // @Builder.Default로 regrading=false가 기본값

        when(submissionRepository.findById(1L)).thenReturn(Optional.of(submission));
        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        submissionService.regradeSubmission(1L);

        // then
        assertThat(submission.getRegrading()).isTrue();
        verify(submissionRepository).save(submission);
    }

    @Test
    void regradeSubmission_이미_재채점_중이면_400_예외가_발생한다() {
        // given
        Submission submission = Submission.builder()
                .id(1L)
                .regrading(true)
                .submittedAnswer("답안")
                .build();

        when(submissionRepository.findById(1L)).thenReturn(Optional.of(submission));

        // when & then
        assertThatThrownBy(() -> submissionService.regradeSubmission(1L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode().value()).isEqualTo(400);
                });

        // save가 호출되지 않아야 함 (상태 변경 없음)
        verify(submissionRepository, never()).save(any());
    }

    @Test
    void regradeSubmission_존재하지_않는_submission이면_404_예외가_발생한다() {
        // given
        when(submissionRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> submissionService.regradeSubmission(999L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode().value()).isEqualTo(404);
                });
    }

    // --- 전체 재채점 ---

    @Test
    void regradeAllSubmissions_정상_전체_재채점_시_모든_submission이_regrading_true이고_count를_반환한다() {
        // given
        Submission sub1 = Submission.builder().id(1L).submittedAnswer("답안1").build();
        Submission sub2 = Submission.builder().id(2L).submittedAnswer("답안2").build();
        Submission sub3 = Submission.builder().id(3L).submittedAnswer("답안3").build();

        when(submissionRepository.findByExamineeIdAndProblemExamId(10L, 20L))
                .thenReturn(List.of(sub1, sub2, sub3));
        // when
        int count = submissionService.regradeAllSubmissions(10L, 20L);

        // then
        assertThat(count).isEqualTo(3);
        assertThat(sub1.getRegrading()).isTrue();
        assertThat(sub2.getRegrading()).isTrue();
        assertThat(sub3.getRegrading()).isTrue();
        verify(submissionRepository).saveAll(List.of(sub1, sub2, sub3));
    }

    @Test
    void regradeAllSubmissions_재채점_중인_submission이_있으면_400_예외가_발생한다() {
        // given
        Submission sub1 = Submission.builder().id(1L).submittedAnswer("답안1").build();
        Submission sub2 = Submission.builder().id(2L).regrading(true).submittedAnswer("답안2").build();

        when(submissionRepository.findByExamineeIdAndProblemExamId(10L, 20L))
                .thenReturn(List.of(sub1, sub2));

        // when & then
        assertThatThrownBy(() -> submissionService.regradeAllSubmissions(10L, 20L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode().value()).isEqualTo(400);
                });

        // saveAll이 호출되지 않아야 함 (상태 변경 없음)
        verify(submissionRepository, never()).saveAll(any());
    }

    @Test
    void regradeAllSubmissions_submission이_없으면_404_예외가_발생한다() {
        // given
        when(submissionRepository.findByExamineeIdAndProblemExamId(10L, 20L))
                .thenReturn(Collections.emptyList());

        // when & then
        assertThatThrownBy(() -> submissionService.regradeAllSubmissions(10L, 20L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode().value()).isEqualTo(404);
                });
    }

    @Test
    void regradeSubmission_정상_시_기존_점수와_피드백이_유지된다() {
        // given — 기존 채점 결과가 있는 submission
        Submission submission = Submission.builder()
                .id(1L)
                .submittedAnswer("답안")
                .earnedScore(8)
                .feedback("잘 작성했습니다")
                .annotatedAnswer("[정답]답안[/정답]")
                .isCorrect(true)
                .build();

        when(submissionRepository.findById(1L)).thenReturn(Optional.of(submission));
        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        submissionService.regradeSubmission(1L);

        // then — regrading만 true로 변경, 기존 채점 결과는 그대로 유지
        assertThat(submission.getRegrading()).isTrue();
        assertThat(submission.getEarnedScore()).isEqualTo(8);
        assertThat(submission.getFeedback()).isEqualTo("잘 작성했습니다");
        assertThat(submission.getAnnotatedAnswer()).isEqualTo("[정답]답안[/정답]");
        assertThat(submission.getIsCorrect()).isTrue();
    }
}
