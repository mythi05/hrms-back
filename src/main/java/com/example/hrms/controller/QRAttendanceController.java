package com.example.hrms.controller;

import com.example.hrms.dto.AttendanceDashboardDTO;
import com.example.hrms.dto.AttendanceQRDTO;
import com.example.hrms.dto.CreateQRDTO;
import com.example.hrms.dto.QRScanRequestDTO;
import com.example.hrms.dto.QRScanResponseDTO;
import com.example.hrms.entity.Employee;
import com.example.hrms.entity.QRScanLog;
import com.example.hrms.repository.EmployeeRepository;
import com.example.hrms.service.QRAttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/qr-attendance")
@RequiredArgsConstructor
@CrossOrigin("*")
public class QRAttendanceController {

    private final QRAttendanceService qrAttendanceService;
    private final EmployeeRepository employeeRepository;

    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @PostMapping("/admin/qr")
    public ResponseEntity<AttendanceQRDTO> createQR(@Valid @RequestBody CreateQRDTO dto, Authentication authentication) {
        Long creatorId = getEmployeeId(authentication);
        return ResponseEntity.ok(qrAttendanceService.createQR(dto, creatorId));
    }

    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @GetMapping("/admin/qr/{id}")
    public ResponseEntity<AttendanceQRDTO> getQR(@PathVariable Long id) {
        return ResponseEntity.ok(qrAttendanceService.getQRById(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @GetMapping("/admin/qr")
    public ResponseEntity<List<AttendanceQRDTO>> getQRsByDate(@RequestParam LocalDate date) {
        return ResponseEntity.ok(qrAttendanceService.getQRsByDate(date));
    }

    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @GetMapping("/admin/qr/active")
    public ResponseEntity<List<AttendanceQRDTO>> getActiveQRs(@RequestParam LocalDate date, @RequestParam(required = false) LocalTime time) {
        return ResponseEntity.ok(qrAttendanceService.getActiveQRs(date, time != null ? time : LocalTime.now()));
    }

    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @PostMapping("/admin/qr/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        qrAttendanceService.deactivateQR(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/qr/image")
    public ResponseEntity<ByteArrayResource> qrImage(@RequestParam String qrCode, @RequestParam(defaultValue = "320") int w, @RequestParam(defaultValue = "320") int h) {
        byte[] png = qrAttendanceService.generateQRImage(qrCode, w, h);
        ByteArrayResource resource = new ByteArrayResource(png);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=attendance-qr.png")
                .contentType(MediaType.IMAGE_PNG)
                .contentLength(png.length)
                .body(resource);
    }

    @PostMapping("/scan")
    public ResponseEntity<QRScanResponseDTO> scan(@Valid @RequestBody QRScanRequestDTO request, Authentication authentication) {
        Long employeeId = getEmployeeId(authentication);
        return ResponseEntity.ok(qrAttendanceService.scanQR(request, employeeId));
    }

    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @GetMapping("/admin/dashboard")
    public ResponseEntity<AttendanceDashboardDTO> dashboard(@RequestParam(required = false) LocalDate date) {
        return ResponseEntity.ok(qrAttendanceService.getDashboardData(date != null ? date : LocalDate.now()));
    }

    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @GetMapping("/admin/scans/recent")
    public ResponseEntity<List<QRScanLog.QRScanLogDTO>> recent(@RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(qrAttendanceService.getRecentScans(limit));
    }

    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @GetMapping("/admin/scans/history")
    public ResponseEntity<List<QRScanLog.QRScanLogDTO>> history(
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) QRScanLog.ScanStatus status
    ) {
        return ResponseEntity.ok(qrAttendanceService.getScanHistory(date != null ? date : LocalDate.now(), status));
    }

    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @GetMapping("/admin/export")
    public ResponseEntity<ByteArrayResource> export(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(defaultValue = "csv") String format
    ) {
        byte[] data = qrAttendanceService.exportAttendanceData(startDate, endDate, format);
        ByteArrayResource resource = new ByteArrayResource(data);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=qr_attendance_export.csv")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(data.length)
                .body(resource);
    }

    private Long getEmployeeId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return null;
        }
        String username = authentication.getName();
        Employee employee = employeeRepository.findByUsername(username).orElse(null);
        return employee != null ? employee.getId() : null;
    }
}
