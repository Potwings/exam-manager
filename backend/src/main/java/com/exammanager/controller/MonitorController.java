package com.exammanager.controller;

import com.exammanager.dto.ExamSessionMonitorResponse;
import com.exammanager.service.MonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/monitor")
@RequiredArgsConstructor
public class MonitorController {

    private final MonitorService monitorService;

    @GetMapping("/sessions")
    public List<ExamSessionMonitorResponse> getSessions(@RequestParam Long examId) {
        return monitorService.getSessionsByExam(examId);
    }
}
