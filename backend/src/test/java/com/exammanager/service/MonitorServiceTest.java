package com.exammanager.service;

import com.exammanager.dto.ExamSessionMonitorResponse;
import com.exammanager.entity.Exam;
import com.exammanager.entity.ExamSession;
import com.exammanager.entity.Examinee;
import com.exammanager.repository.ExamSessionRepository;
import com.exammanager.repository.SubmissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonitorServiceTest {

    @Mock
    private ExamSessionRepository examSessionRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private ExamService examService;

    @InjectMocks
    private MonitorService monitorService;

    /**
     * 시간 제한 있는 시험에서 아직 제출하지 않고 시간이 남은 수험자
     * -> status: IN_PROGRESS, remainingSeconds: 양수
     */
    @Test
    void getSessionsByExam_inProgress_returnsInProgressWithRemainingSeconds() {
        // given
        Long examId = 1L;
        Exam exam = Exam.builder().id(examId).timeLimit(60).build(); // 60분 제한
        Examinee examinee = Examinee.builder().id(10L).name("홍길동").birthDate(LocalDate.of(1990, 1, 1)).build();
        ExamSession session = ExamSession.builder()
                .id(100L)
                .examinee(examinee)
                .exam(exam)
                .startedAt(LocalDateTime.now().minusMinutes(10)) // 10분 전 시작 -> 50분 남음
                .build();

        when(examService.findById(examId)).thenReturn(exam);
        when(examSessionRepository.findByExamIdWithExaminee(examId)).thenReturn(List.of(session));
        when(submissionRepository.existsByExamineeIdAndProblemExamId(10L, examId)).thenReturn(false);

        // when
        List<ExamSessionMonitorResponse> result = monitorService.getSessionsByExam(examId);

        // then
        assertThat(result).hasSize(1);
        ExamSessionMonitorResponse response = result.get(0);
        assertThat(response.getExamineeId()).isEqualTo(10L);
        assertThat(response.getExamineeName()).isEqualTo("홍길동");
        assertThat(response.getExamineeBirthDate()).isEqualTo(LocalDate.of(1990, 1, 1));
        assertThat(response.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(response.getRemainingSeconds()).isGreaterThan(0);
        assertThat(response.getStartedAt()).isNotNull();
    }

    /**
     * 답안을 이미 제출한 수험자
     * -> status: SUBMITTED, remainingSeconds: null
     */
    @Test
    void getSessionsByExam_submitted_returnsSubmittedWithNullRemaining() {
        // given
        Long examId = 1L;
        Exam exam = Exam.builder().id(examId).timeLimit(60).build();
        Examinee examinee = Examinee.builder().id(10L).name("김영희").birthDate(LocalDate.of(1995, 5, 15)).build();
        ExamSession session = ExamSession.builder()
                .id(100L)
                .examinee(examinee)
                .exam(exam)
                .startedAt(LocalDateTime.now().minusMinutes(30))
                .build();

        when(examService.findById(examId)).thenReturn(exam);
        when(examSessionRepository.findByExamIdWithExaminee(examId)).thenReturn(List.of(session));
        when(submissionRepository.existsByExamineeIdAndProblemExamId(10L, examId)).thenReturn(true);

        // when
        List<ExamSessionMonitorResponse> result = monitorService.getSessionsByExam(examId);

        // then
        assertThat(result).hasSize(1);
        ExamSessionMonitorResponse response = result.get(0);
        assertThat(response.getStatus()).isEqualTo("SUBMITTED");
        assertThat(response.getRemainingSeconds()).isNull();
    }

    /**
     * 시간 제한 있는 시험에서 제출하지 않고 시간이 만료된 수험자
     * -> status: TIME_EXPIRED, remainingSeconds: 0
     */
    @Test
    void getSessionsByExam_timeExpired_returnsTimeExpiredWithZeroRemaining() {
        // given
        Long examId = 1L;
        Exam exam = Exam.builder().id(examId).timeLimit(60).build();
        Examinee examinee = Examinee.builder().id(10L).name("이철수").birthDate(LocalDate.of(2000, 12, 25)).build();
        ExamSession session = ExamSession.builder()
                .id(100L)
                .examinee(examinee)
                .exam(exam)
                .startedAt(LocalDateTime.now().minusMinutes(120)) // 120분 전 시작 -> 60분 제한 초과
                .build();

        when(examService.findById(examId)).thenReturn(exam);
        when(examSessionRepository.findByExamIdWithExaminee(examId)).thenReturn(List.of(session));
        when(submissionRepository.existsByExamineeIdAndProblemExamId(10L, examId)).thenReturn(false);

        // when
        List<ExamSessionMonitorResponse> result = monitorService.getSessionsByExam(examId);

        // then
        assertThat(result).hasSize(1);
        ExamSessionMonitorResponse response = result.get(0);
        assertThat(response.getStatus()).isEqualTo("TIME_EXPIRED");
        assertThat(response.getRemainingSeconds()).isEqualTo(0L);
    }

    /**
     * 시간 제한 없는 시험에서 응시 중인 수험자
     * -> status: IN_PROGRESS, remainingSeconds: null
     */
    @Test
    void getSessionsByExam_noTimeLimit_returnsInProgressWithNullRemaining() {
        // given
        Long examId = 1L;
        Exam exam = Exam.builder().id(examId).timeLimit(null).build(); // 시간 제한 없음
        Examinee examinee = Examinee.builder().id(10L).name("박지수").birthDate(LocalDate.of(1998, 3, 10)).build();
        ExamSession session = ExamSession.builder()
                .id(100L)
                .examinee(examinee)
                .exam(exam)
                .startedAt(LocalDateTime.now().minusMinutes(45))
                .build();

        when(examService.findById(examId)).thenReturn(exam);
        when(examSessionRepository.findByExamIdWithExaminee(examId)).thenReturn(List.of(session));
        when(submissionRepository.existsByExamineeIdAndProblemExamId(10L, examId)).thenReturn(false);

        // when
        List<ExamSessionMonitorResponse> result = monitorService.getSessionsByExam(examId);

        // then
        assertThat(result).hasSize(1);
        ExamSessionMonitorResponse response = result.get(0);
        assertThat(response.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(response.getRemainingSeconds()).isNull();
    }

    /**
     * 세션이 없는 시험 조회
     * -> 빈 배열 반환
     */
    @Test
    void getSessionsByExam_noSessions_returnsEmptyList() {
        // given
        Long examId = 1L;
        Exam exam = Exam.builder().id(examId).timeLimit(60).build();

        when(examService.findById(examId)).thenReturn(exam);
        when(examSessionRepository.findByExamIdWithExaminee(examId)).thenReturn(Collections.emptyList());

        // when
        List<ExamSessionMonitorResponse> result = monitorService.getSessionsByExam(examId);

        // then
        assertThat(result).isEmpty();
    }

    /**
     * 여러 수험자가 있을 때 startedAt 역순 정렬 확인
     * -> 최근 진입자가 리스트 앞에 위치
     */
    @Test
    void getSessionsByExam_multipleSessions_sortedByStartedAtDescending() {
        // given
        Long examId = 1L;
        Exam exam = Exam.builder().id(examId).timeLimit(60).build();

        Examinee examinee1 = Examinee.builder().id(10L).name("첫번째").birthDate(LocalDate.of(1990, 1, 1)).build();
        Examinee examinee2 = Examinee.builder().id(11L).name("두번째").birthDate(LocalDate.of(1991, 2, 2)).build();
        Examinee examinee3 = Examinee.builder().id(12L).name("세번째").birthDate(LocalDate.of(1992, 3, 3)).build();

        ExamSession session1 = ExamSession.builder()
                .id(100L).examinee(examinee1).exam(exam)
                .startedAt(LocalDateTime.now().minusMinutes(30)) // 30분 전 (가장 먼저 진입)
                .build();
        ExamSession session2 = ExamSession.builder()
                .id(101L).examinee(examinee2).exam(exam)
                .startedAt(LocalDateTime.now().minusMinutes(20)) // 20분 전
                .build();
        ExamSession session3 = ExamSession.builder()
                .id(102L).examinee(examinee3).exam(exam)
                .startedAt(LocalDateTime.now().minusMinutes(10)) // 10분 전 (가장 최근 진입)
                .build();

        when(examService.findById(examId)).thenReturn(exam);
        when(examSessionRepository.findByExamIdWithExaminee(examId)).thenReturn(List.of(session1, session2, session3));
        when(submissionRepository.existsByExamineeIdAndProblemExamId(anyLong(), eq(examId))).thenReturn(false);

        // when
        List<ExamSessionMonitorResponse> result = monitorService.getSessionsByExam(examId);

        // then
        assertThat(result).hasSize(3);
        // 최근 진입자가 먼저 (startedAt 역순)
        assertThat(result.get(0).getExamineeName()).isEqualTo("세번째");
        assertThat(result.get(1).getExamineeName()).isEqualTo("두번째");
        assertThat(result.get(2).getExamineeName()).isEqualTo("첫번째");
    }
}
