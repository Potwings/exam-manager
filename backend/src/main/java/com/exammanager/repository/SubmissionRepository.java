package com.exammanager.repository;

import com.exammanager.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByExamineeId(Long examineeId);
    List<Submission> findByProblemExamId(Long examId);
    List<Submission> findByExamineeIdAndProblemExamId(Long examineeId, Long examId);
}
