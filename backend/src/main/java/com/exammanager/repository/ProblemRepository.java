package com.exammanager.repository;

import com.exammanager.entity.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProblemRepository extends JpaRepository<Problem, Long> {
    List<Problem> findByExamIdOrderByProblemNumber(Long examId);
}
