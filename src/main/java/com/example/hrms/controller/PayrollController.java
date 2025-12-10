package com.example.hrms.controller;

import com.example.hrms.dto.PayrollDTO;
import com.example.hrms.service.PayrollService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/payroll")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;

    // Endpoint cho nhân viên xem lương hiện tại
    @GetMapping("/employee/{employeeId}/current")
    public ResponseEntity<PayrollDTO> getCurrentPayroll(@PathVariable Long employeeId,
                                                        @RequestParam(required = false) Integer month,
                                                        @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(payrollService.getPayrollForEmployeeMonth(employeeId, month, year));
    }

    // Lịch sử lương N tháng gần nhất
    @GetMapping("/employee/{employeeId}/history")
    public ResponseEntity<List<PayrollDTO>> getHistory(@PathVariable Long employeeId,
                                                       @RequestParam(defaultValue = "6") int months) {
        return ResponseEntity.ok(payrollService.getPayrollHistoryForEmployee(employeeId, months));
    }

    // Admin: tạo/cập nhật bảng lương
    @PostMapping("/admin")
    public ResponseEntity<PayrollDTO> createOrUpdate(@RequestBody PayrollDTO dto) {
        return ResponseEntity.ok(payrollService.createOrUpdate(dto));
    }

    // Admin: danh sách lương theo tháng
    @GetMapping("/admin/month")
    public ResponseEntity<List<PayrollDTO>> getByMonth(@RequestParam(required = false) Integer month,
                                                       @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(payrollService.getPayrollForMonth(month, year));
    }

    // Admin: tất cả bảng lương
    @GetMapping("/admin")
    public ResponseEntity<List<PayrollDTO>> getAll() {
        return ResponseEntity.ok(payrollService.getAllPayroll());
    }

    // Đánh dấu đã chi trả
    @PostMapping("/admin/{id}/mark-paid")
    public ResponseEntity<PayrollDTO> markPaid(@PathVariable Long id) {
        return ResponseEntity.ok(payrollService.markPaid(id));
    }

    // Đánh dấu chưa chi trả
    @PostMapping("/admin/{id}/mark-pending")
    public ResponseEntity<PayrollDTO> markPending(@PathVariable Long id) {
        return ResponseEntity.ok(payrollService.markPending(id));
    }

    // Tự động tính lương dựa trên lương cơ bản và dữ liệu chấm công
    @GetMapping("/admin/calculate")
    public ResponseEntity<PayrollDTO> calculatePayroll(
            @RequestParam Long employeeId,
            @RequestParam Integer month,
            @RequestParam Integer year,
            @RequestParam BigDecimal basicSalary) {
        return ResponseEntity.ok(payrollService.calculatePayroll(employeeId, month, year, basicSalary));
    }
}
