package com.exammanager.service;

import com.exammanager.entity.Examinee;
import com.exammanager.repository.ExamineeRepository;
import com.exammanager.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExamineeService {

    private final ExamineeRepository examineeRepository;
    private final SubmissionRepository submissionRepository;

    @Transactional
    public Examinee loginOrCreate(String name, LocalDate birthDate) {
        Optional<Examinee> existing = examineeRepository.findByNameAndBirthDate(name, birthDate);

        if (existing.isPresent()) {
            // 기존 수험자 → 활성 시험에 대한 재시험 방지 검증
            Examinee examinee = existing.get();
            if (submissionRepository.countByExamineeIdAndActiveExam(examinee.getId()) > 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "이미 응시 완료한 시험입니다");
            }
            return examinee;
        }

        // 신규 수험자 → 제출 기록이 있을 수 없으므로 검증 불필요, 바로 생성
        try {
            return examineeRepository.save(
                    Examinee.builder().name(name).birthDate(birthDate).build());
        } catch (DataIntegrityViolationException e) {
            // 동시 요청으로 중복 insert 실패 시, 이미 생성된 레코드를 재조회
            log.warn("수험자 동시 로그인 충돌 후 재조회: name={}", name);
            return examineeRepository
                    .findByNameAndBirthDate(name, birthDate)
                    .orElseThrow(() -> {
                        log.error("수험자 동시 로그인 충돌 후 재조회 실패: name={}", name);
                        return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
                    });
        }
    }
}
