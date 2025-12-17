package com.example.hrms.dto;

import com.example.hrms.entity.AttendanceQR;
import com.example.hrms.entity.QRScanLog;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Data
public class AttendanceQRDTO {
    
    private Long id;
    private String qrCode;
    private LocalDate date;
    private AttendanceQR.ShiftType shiftType;
    private QRScanLog.ScanType scanType;
    private LocalTime validFrom;
    private LocalTime validTo;
    private Boolean isActive;
    private Long createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Additional fields for display
    private String shiftTypeDisplay;
    private String scanTypeDisplay;
    private String timeRange;
    private boolean isValid;
}
