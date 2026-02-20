package com.exammanager.repository;

import com.exammanager.entity.Examinee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface ExamineeRepository extends JpaRepository<Examinee, Long> {
    Optional<Examinee> findByNameAndBirthDate(String name, LocalDate birthDate);
}
