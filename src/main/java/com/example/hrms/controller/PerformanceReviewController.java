package com.example.hrms.controller;

import com.example.hrms.dto.PerformanceReviewDTO;
import com.example.hrms.entity.Employee;
import com.example.hrms.repository.EmployeeRepository;
import com.example.hrms.service.PerformanceReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/performance")
@RequiredArgsConstructor
public class PerformanceReviewController {

    private final PerformanceReviewService service;
    private final EmployeeRepository employeeRepository;

    @GetMapping("/{id}")
    public ResponseEntity<PerformanceReviewDTO> getById(@PathVariable Long id) {
        PerformanceReviewDTO dto = service.getById(id);
        if (dto == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(dto);
    }

    @GetMapping
    public ResponseEntity<List<PerformanceReviewDTO>> list(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long reviewerId,
            @RequestParam(required = false) String period
    ) {
        if (employeeId != null) {
            return ResponseEntity.ok(service.listByEmployee(employeeId));
        }
        if (reviewerId != null) {
            return ResponseEntity.ok(service.listByReviewer(reviewerId));
        }
        // nếu không có param, trả về tất cả theo period (admin)
        return ResponseEntity.ok(service.listAllByPeriod(period));
    }

    @GetMapping("/my")
    public ResponseEntity<List<PerformanceReviewDTO>> myReviews() {
        // Lấy user hiện tại
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        return employeeRepository.findByUsername(username)
                .map(emp -> ResponseEntity.ok(service.listByEmployee(emp.getId())))
                .orElse(ResponseEntity.ok(List.of()));
    }

    // Admin routes
    @GetMapping("/admin")
    public ResponseEntity<List<PerformanceReviewDTO>> adminList(@RequestParam(required = false) String period) {
        return ResponseEntity.ok(service.listAllByPeriod(period));
    }

    @PostMapping("/admin")
    public ResponseEntity<PerformanceReviewDTO> adminCreateOrUpdate(@RequestBody PerformanceReviewDTO dto) {
        PerformanceReviewDTO saved = service.createOrUpdate(dto);
        return ResponseEntity.ok(saved);
    }

    @PostMapping
    public ResponseEntity<PerformanceReviewDTO> createOrUpdate(@RequestBody PerformanceReviewDTO dto) {
        PerformanceReviewDTO saved = service.createOrUpdate(dto);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<?> submit(@PathVariable Long id) {
        boolean ok = service.submit(id);
        if (!ok) return ResponseEntity.status(403).body("Không có quyền hoặc không tìm thấy");
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Long id) {
        boolean ok = service.approve(id);
        if (!ok) return ResponseEntity.status(403).body("Không có quyền hoặc không tìm thấy");
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        boolean ok = service.delete(id);
        if (!ok) return ResponseEntity.status(403).body("Không có quyền hoặc không tìm thấy");
        return ResponseEntity.ok().build();
    }

    // Helper: get employee info by username (frontend có thể dùng để map id->name)
    @GetMapping("/employee/by-username")
    public ResponseEntity<Employee> findEmployeeByUsername(@RequestParam String username) {
        return employeeRepository.findByUsername(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
