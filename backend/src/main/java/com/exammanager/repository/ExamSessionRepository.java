package com.exammanager.repository;

import com.exammanager.entity.ExamSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExamSessionRepository extends JpaRepository<ExamSession, Long> {
    Optional<ExamSession> findByExamineeIdAndExamId(Long examineeId, Long examId);
}
