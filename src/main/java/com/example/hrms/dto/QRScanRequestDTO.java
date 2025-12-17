package com.example.hrms.dto;

import com.example.hrms.entity.QRScanLog;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class QRScanRequestDTO {
    
    @NotBlank(message = "Mã QR không được để trống")
    private String qrCode;
    
    private QRScanLog.ScanType scanType;
    
    // Client information for logging
    private String ipAddress;
    private String userAgent;
}
