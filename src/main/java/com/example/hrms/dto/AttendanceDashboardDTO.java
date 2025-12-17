package com.example.hrms.dto;

import com.example.hrms.entity.QRScanLog;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AttendanceDashboardDTO {
    
    private LocalDate date;
    
    // QR Statistics
    private long totalQRs;
    private long activeQRs;
    private long totalScans;
    private long successfulScans;
    private long failedScans;
    
    // Employee Statistics
    private long totalEmployees;
    private long checkedInEmployees;
    private long pendingEmployees;
    private long lateEmployees;
    
    // Recent Activities
    private List<RecentScanDTO> recentScans;
    private List<AttendanceQRDTO> activeQRsList;
    
    // Shift Statistics
    private ShiftStatsDTO morningShift;
    private ShiftStatsDTO afternoonShift;
    private ShiftStatsDTO nightShift;
    
    @Data
    public static class RecentScanDTO {
        private String employeeName;
        private String employeeId;
        private QRScanLog.ScanType scanType;
        private QRScanLog.ScanStatus status;
        private LocalDateTime scanTime;
        private String shiftType;
    }
    
    @Data
    public static class ShiftStatsDTO {
        private String shiftName;
        private long totalEmployees;
        private long checkedIn;
        private long pending;
        private LocalTime validFrom;
        private LocalTime validTo;
        private boolean isActive;
    }
}
