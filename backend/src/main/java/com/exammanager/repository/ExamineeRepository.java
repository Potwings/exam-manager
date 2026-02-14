package com.exammanager.repository;

import com.exammanager.entity.Examinee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamineeRepository extends JpaRepository<Examinee, Long> {
}
