package com.exammanager.controller;

import com.exammanager.dto.ExamineeLoginRequest;
import com.exammanager.dto.ExamineeResponse;
import com.exammanager.entity.Examinee;
import com.exammanager.repository.ExamineeRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/examinees")
@RequiredArgsConstructor
public class ExamineeController {

    private final ExamineeRepository examineeRepository;

    @PostMapping("/login")
    public ResponseEntity<ExamineeResponse> login(@Valid @RequestBody ExamineeLoginRequest request) {
        // 이름+생년월일로 기존 수험자 조회 → 없으면 신규 생성 (find-or-create)
        Examinee examinee;
        try {
            examinee = examineeRepository
                    .findByNameAndBirthDate(request.getName(), request.getBirthDate())
                    .orElseGet(() -> examineeRepository.save(
                            Examinee.builder()
                                    .name(request.getName())
                                    .birthDate(request.getBirthDate())
                                    .build()
                    ));
        } catch (DataIntegrityViolationException e) {
            // 동시 요청으로 중복 insert 실패 시, 이미 생성된 레코드를 재조회
            examinee = examineeRepository
                    .findByNameAndBirthDate(request.getName(), request.getBirthDate())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "수험자 조회 실패: " + request.getName()));
        }
        return ResponseEntity.ok(ExamineeResponse.from(examinee));
    }
}
