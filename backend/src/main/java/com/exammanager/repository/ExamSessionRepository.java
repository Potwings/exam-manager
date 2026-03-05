package com.exammanager.repository;

import com.exammanager.entity.ExamSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ExamSessionRepository extends JpaRepository<ExamSession, Long> {
    Optional<ExamSession> findByExamineeIdAndExamId(Long examineeId, Long examId);

    @Query("SELECT s FROM ExamSession s JOIN FETCH s.examinee WHERE s.exam.id = :examId")
    List<ExamSession> findByExamIdWithExaminee(@Param("examId") Long examId);
}
