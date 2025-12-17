package com.example.hrms.service;

import com.example.hrms.dto.*;
import com.example.hrms.entity.AttendanceQR;
import com.example.hrms.entity.QRScanLog;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface QRAttendanceService {

    // QR Management
    AttendanceQRDTO createQR(CreateQRDTO dto, Long createdBy);
    
    AttendanceQRDTO getQRById(Long id);
    
    List<AttendanceQRDTO> getQRsByDate(LocalDate date);
    
    List<AttendanceQRDTO> getActiveQRs(LocalDate date, LocalTime currentTime);
    
    void deactivateQR(Long id);
    
    byte[] generateQRImage(String qrCode, int width, int height);
    
    // QR Scanning
    QRScanResponseDTO scanQR(QRScanRequestDTO request, Long employeeId);
    
    // Dashboard & Statistics
    AttendanceDashboardDTO getDashboardData(LocalDate date);
    
    List<QRScanLog.QRScanLogDTO> getRecentScans(int limit);
    
    List<QRScanLog.QRScanLogDTO> getScanHistory(LocalDate date, QRScanLog.ScanStatus status);
    
    // Export
    byte[] exportAttendanceData(LocalDate startDate, LocalDate endDate, String format);
    
    // Validation
    boolean isValidQR(String qrCode, AttendanceQR.ShiftType shiftType, LocalTime currentTime);
    
    boolean hasEmployeeScannedToday(Long employeeId, String qrCode, QRScanLog.ScanType scanType);
}
