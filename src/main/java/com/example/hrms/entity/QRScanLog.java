package com.example.hrms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "qr_scan_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QRScanLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "qr_code", nullable = false)
    private String qrCode;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScanType scanType;

    @Enumerated(EnumType.STRING)
    @Column(name = "shift_type", nullable = false)
    private AttendanceQR.ShiftType shiftType;

    @Column(name = "scan_time", nullable = false)
    private LocalDateTime scanTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ScanStatus status = ScanStatus.SUCCESS;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum ScanType {
        CHECK_IN, CHECK_OUT
    }

    public enum ScanStatus {
        SUCCESS, QR_NOT_FOUND, QR_EXPIRED, ALREADY_SCANNED, INVALID_SHIFT, EMPLOYEE_NOT_FOUND
    }

    @Data
    public static class QRScanLogDTO {
        private Long id;
        private String qrCode;
        private Long employeeId;
        private ScanType scanType;
        private AttendanceQR.ShiftType shiftType;
        private LocalDateTime scanTime;
        private ScanStatus status;
        private String errorMessage;
    }
}
