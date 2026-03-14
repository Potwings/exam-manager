package com.exammanager.controller;

import com.exammanager.dto.ExamineeLoginRequest;
import com.exammanager.dto.ExamineeResponse;
import com.exammanager.entity.Examinee;
import com.exammanager.service.ExamineeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/examinees")
@RequiredArgsConstructor
public class ExamineeController {

    private final ExamineeService examineeService;

    @PostMapping("/login")
    public ResponseEntity<ExamineeResponse> login(
            @Valid @RequestBody ExamineeLoginRequest request) {
        Examinee examinee = examineeService.loginOrCreate(
                request.getName(), request.getBirthDate());
        return ResponseEntity.ok(ExamineeResponse.from(examinee));
    }
}
