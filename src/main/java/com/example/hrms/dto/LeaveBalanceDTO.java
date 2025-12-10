package com.example.hrms.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class LeaveBalanceDTO {
    
    private Long id;
    private Long employeeId;
    private Integer year;
    private String leaveType;
    private BigDecimal totalDays;
    private BigDecimal usedDays;
    private BigDecimal remainingDays;
}
