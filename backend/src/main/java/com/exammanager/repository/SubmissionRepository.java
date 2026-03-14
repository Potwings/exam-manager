package com.exammanager.repository;

import com.exammanager.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByExamineeId(Long examineeId);
    List<Submission> findByProblemExamId(Long examId);
    List<Submission> findByExamineeIdAndProblemExamId(Long examineeId, Long examId);
    Optional<Submission> findByExamineeIdAndProblemId(Long examineeId, Long problemId);

    boolean existsByProblemExamId(Long examId);
    boolean existsByExamineeIdAndProblemExamId(Long examineeId, Long examId);
    @Query("SELECT COUNT(s) FROM Submission s " +
           "WHERE s.examinee.id = :examineeId " +
           "AND s.problem.exam.active = true " +
           "AND s.problem.exam.deleted = false")
    long countByExamineeIdAndActiveExam(@Param("examineeId") Long examineeId);

    @Query("SELECT DISTINCT s.examinee.id FROM Submission s WHERE s.problem.exam.id = :examId")
    Set<Long> findSubmittedExamineeIdsByExamId(@Param("examId") Long examId);
}
