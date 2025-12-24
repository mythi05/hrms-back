package com.example.hrms.controller;

import com.example.hrms.dto.PerformanceReviewDTO;
import com.example.hrms.service.PerformanceReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/performance")
@RequiredArgsConstructor
public class AdminPerformanceController {

    private final PerformanceReviewService service;

    @GetMapping
    public ResponseEntity<List<PerformanceReviewDTO>> list(@RequestParam(required = false) String period) {
        return ResponseEntity.ok(service.listAllByPeriod(period));
    }

    @PostMapping
    public ResponseEntity<PerformanceReviewDTO> createOrUpdate(@RequestBody PerformanceReviewDTO dto) {
        PerformanceReviewDTO saved = service.createOrUpdate(dto);
        return ResponseEntity.ok(saved);
    }
}
