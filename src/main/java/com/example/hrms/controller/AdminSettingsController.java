package com.example.hrms.controller;

import com.example.hrms.dto.AdminSettingsDTO;
import com.example.hrms.dto.DashboardStatsDTO;
import com.example.hrms.repository.DepartmentRepository;
import com.example.hrms.service.AdminSettingsService;
import com.example.hrms.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
public class AdminSettingsController {

    private final AdminSettingsService adminSettingsService;
    private final DashboardService dashboardService;
    private final DepartmentRepository departmentRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminSettingsDTO> get() {
        return ResponseEntity.ok(adminSettingsService.get());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminSettingsDTO> update(@RequestBody AdminSettingsDTO dto) {
        return ResponseEntity.ok(adminSettingsService.update(dto));
    }

    @GetMapping("/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> overview() {
        DashboardStatsDTO stats = dashboardService.getAdminStats();

        long deptCount = departmentRepository.count();

        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();

        File root = new File(".");
        long totalBytes = root.getTotalSpace();
        long freeBytes = root.getFreeSpace();
        long usedBytes = totalBytes - freeBytes;

        Map<String, Object> map = new HashMap<>();
        map.put("totalEmployees", stats.getTotalEmployees());
        map.put("departmentCount", deptCount);
        map.put("uptimeMs", uptimeMs);
        map.put("diskUsedBytes", usedBytes);
        map.put("diskTotalBytes", totalBytes);
        return ResponseEntity.ok(map);
    }
}
