package com.example.hrms.controller;

import com.example.hrms.dto.DashboardStatsDTO;
import com.example.hrms.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    // Admin Dashboard Stats
    @GetMapping("/admin/stats")
    public ResponseEntity<DashboardStatsDTO> getAdminStats() {
        return ResponseEntity.ok(dashboardService.getAdminStats());
    }

    // Employee Dashboard Stats
    @GetMapping("/employee/{employeeId}/stats")
    public ResponseEntity<?> getEmployeeStats(@PathVariable Long employeeId) {
        return ResponseEntity.ok(dashboardService.getEmployeeStats(employeeId));
    }

    // Recent Activities for Admin
    @GetMapping("/admin/recent-activities")
    public ResponseEntity<?> getRecentActivities() {
        return ResponseEntity.ok(dashboardService.getRecentActivities());
    }

    // Department Distribution
    @GetMapping("/admin/department-distribution")
    public ResponseEntity<?> getDepartmentDistribution() {
        return ResponseEntity.ok(dashboardService.getDepartmentDistribution());
    }

    // Pending Requests
    @GetMapping("/admin/pending-requests")
    public ResponseEntity<?> getPendingRequests() {
        return ResponseEntity.ok(dashboardService.getPendingRequests());
    }

    // Birthdays this month
    @GetMapping("/admin/birthdays")
    public ResponseEntity<?> getBirthdaysThisMonth() {
        return ResponseEntity.ok(dashboardService.getBirthdaysThisMonth());
    }

    // Attendance statistics for chart
    @GetMapping("/admin/attendance-stats")
    public ResponseEntity<?> getAttendanceStats() {
        return ResponseEntity.ok(dashboardService.getAttendanceStats());
    }

    // Payroll trends for chart
    @GetMapping("/admin/payroll-trends")
    public ResponseEntity<?> getPayrollTrends() {
        return ResponseEntity.ok(dashboardService.getPayrollTrends());
    }

    // Employee payroll trends
    @GetMapping("/employee/{employeeId}/payroll-trends")
    public ResponseEntity<?> getEmployeePayrollTrends(@PathVariable Long employeeId) {
        return ResponseEntity.ok(dashboardService.getEmployeePayrollTrends(employeeId));
    }
}
