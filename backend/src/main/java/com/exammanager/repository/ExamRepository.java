package com.exammanager.repository;

import com.exammanager.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExamRepository extends JpaRepository<Exam, Long> {
    List<Exam> findByDeletedFalse();
    Optional<Exam> findByActiveTrueAndDeletedFalse();
}
