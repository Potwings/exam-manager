package com.exammanager.controller;

import com.exammanager.dto.ExamineeLoginRequest;
import com.exammanager.dto.ExamineeResponse;
import com.exammanager.entity.Examinee;
import com.exammanager.repository.ExamineeRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/examinees")
@RequiredArgsConstructor
public class ExamineeController {

    private final ExamineeRepository examineeRepository;

    @PostMapping("/login")
    public ResponseEntity<ExamineeResponse> login(@Valid @RequestBody ExamineeLoginRequest request) {
        // 이름+생년월일로 기존 수험자 조회 → 없으면 신규 생성 (find-or-create)
        Examinee examinee = examineeRepository
                .findByNameAndBirthDate(request.getName(), request.getBirthDate())
                .orElseGet(() -> examineeRepository.save(
                        Examinee.builder()
                                .name(request.getName())
                                .birthDate(request.getBirthDate())
                                .build()
                ));
        return ResponseEntity.ok(ExamineeResponse.from(examinee));
    }
}
