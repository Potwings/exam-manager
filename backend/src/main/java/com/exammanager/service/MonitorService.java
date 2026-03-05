package com.exammanager.service;

import com.exammanager.config.ExamTimeUtils;
import com.exammanager.dto.ExamSessionMonitorResponse;
import com.exammanager.entity.Exam;
import com.exammanager.entity.ExamSession;
import com.exammanager.entity.Examinee;
import com.exammanager.repository.ExamSessionRepository;
import com.exammanager.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MonitorService {

    private final ExamSessionRepository examSessionRepository;
    private final SubmissionRepository submissionRepository;
    private final ExamService examService;

    /**
     * 특정 시험의 응시 현황을 조회한다.
     * ExamSession을 기반으로 각 수험자의 제출 여부, 남은 시간, 상태를 판별하여 반환한다.
     *
     * @param examId 조회할 시험 ID (존재하지 않으면 NOT_FOUND)
     * @return 수험자별 모니터링 정보 (startedAt 역순 정렬)
     */
    @Transactional(readOnly = true)
    public List<ExamSessionMonitorResponse> getSessionsByExam(Long examId) {
        // ExamService.findById()는 시험이 없으면 ResponseStatusException(NOT_FOUND)을 던짐
        Exam exam = examService.findById(examId);

        List<ExamSession> sessions = examSessionRepository.findByExamIdWithExaminee(examId);

        // 제출 여부를 한 번의 쿼리로 일괄 조회 (N+1 방지)
        Set<Long> submittedExamineeIds = submissionRepository.findSubmittedExamineeIdsByExamId(examId);

        return sessions.stream()
                .map(session -> buildMonitorResponse(session, exam, submittedExamineeIds))
                .sorted(Comparator.comparing(ExamSessionMonitorResponse::getStartedAt).reversed())
                .toList();
    }

    /**
     * 개별 ExamSession에 대해 모니터링 응답을 구성한다.
     * 제출 여부와 시간 제한 상태에 따라 status와 remainingSeconds를 결정한다.
     */
    private ExamSessionMonitorResponse buildMonitorResponse(ExamSession session, Exam exam,
                                                             Set<Long> submittedExamineeIds) {
        Examinee examinee = session.getExaminee();
        boolean hasSubmission = submittedExamineeIds.contains(examinee.getId());

        String status;
        Long remainingSeconds;

        if (hasSubmission) {
            // 제출 완료: 시간 제한 유무와 관계없이 SUBMITTED, remainingSeconds는 null
            status = "SUBMITTED";
            remainingSeconds = null;
        } else if (exam.getTimeLimit() != null) {
            // 시간 제한 있는 시험에서 미제출: 남은 시간에 따라 IN_PROGRESS 또는 TIME_EXPIRED
            long remaining = ExamTimeUtils.calculateRemainingSeconds(session.getStartedAt(), exam.getTimeLimit());
            if (remaining <= 0) {
                status = "TIME_EXPIRED";
                remainingSeconds = 0L;
            } else {
                status = "IN_PROGRESS";
                remainingSeconds = remaining;
            }
        } else {
            // 시간 제한 없는 시험에서 미제출: IN_PROGRESS, remainingSeconds는 null
            status = "IN_PROGRESS";
            remainingSeconds = null;
        }

        return ExamSessionMonitorResponse.builder()
                .examineeId(examinee.getId())
                .examineeName(examinee.getName())
                .examineeBirthDate(examinee.getBirthDate())
                .status(status)
                .remainingSeconds(remainingSeconds)
                .startedAt(session.getStartedAt())
                .build();
    }

}
