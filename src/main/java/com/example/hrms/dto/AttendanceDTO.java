package com.example.hrms.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class AttendanceDTO {
    
    private Long id;
    private Long employeeId;
    private String employeeName;
    private String department;
    private LocalDate date;
    private LocalTime checkIn;
    private LocalTime checkOut;
    private BigDecimal totalHours;
    private String status;
    private String note;
}
