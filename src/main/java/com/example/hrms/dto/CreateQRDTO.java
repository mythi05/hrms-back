package com.example.hrms.dto;

import com.example.hrms.entity.AttendanceQR;
import com.example.hrms.entity.QRScanLog;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class CreateQRDTO {
    
    @NotNull(message = "Ngày không được để trống")
    private LocalDate date;
    
    @NotNull(message = "Loại nhân sự (FULL_TIME) không được để trống")
    private AttendanceQR.ShiftType shiftType;

    @NotNull(message = "Loại quét không được để trống")
    private QRScanLog.ScanType scanType;
    
    @NotNull(message = "Thời gian bắt đầu không được để trống")
    private LocalTime validFrom;
    
    @NotNull(message = "Thời gian kết thúc không được để trống")
    private LocalTime validTo;
}
