package com.exammanager.repository;

import com.exammanager.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByExamineeId(Long examineeId);
    List<Submission> findByProblemExamId(Long examId);
    List<Submission> findByExamineeIdAndProblemExamId(Long examineeId, Long examId);
    Optional<Submission> findByExamineeIdAndProblemId(Long examineeId, Long problemId);

    boolean existsByProblemExamId(Long examId);
    boolean existsByExamineeIdAndProblemExamId(Long examineeId, Long examId);
}
