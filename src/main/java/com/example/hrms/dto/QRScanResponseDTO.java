package com.example.hrms.dto;

import com.example.hrms.entity.QRScanLog;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class QRScanResponseDTO {
    
    private boolean success;
    private String message;
    private QRScanLog.ScanStatus status;
    private QRScanLog.ScanType scanType;
    
    // Attendance information
    private Long attendanceId;
    private LocalDateTime scanTime;
    private LocalTime checkTime;
    private String employeeName;
    private String employeeId;
    private String shiftType;
    
    // QR information
    private String qrCode;
    private String validTimeRange;
    
    public static QRScanResponseDTO success(String message, LocalTime checkTime, String employeeName) {
        QRScanResponseDTO response = new QRScanResponseDTO();
        response.setSuccess(true);
        response.setMessage(message);
        response.setStatus(QRScanLog.ScanStatus.SUCCESS);
        response.setCheckTime(checkTime);
        response.setEmployeeName(employeeName);
        response.setScanTime(LocalDateTime.now());
        return response;
    }
    
    public static QRScanResponseDTO error(QRScanLog.ScanStatus status, String message) {
        QRScanResponseDTO response = new QRScanResponseDTO();
        response.setSuccess(false);
        response.setMessage(message);
        response.setStatus(status);
        response.setScanTime(LocalDateTime.now());
        return response;
    }
}
