package com.example.hrms.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PayrollDTO {
    
    private Long id;
    private Long employeeId;
    private String employeeName;
    private Integer month;
    private Integer year;
    private BigDecimal basicSalary;
    private BigDecimal allowances;
    private BigDecimal bonus;
    private BigDecimal overtimePay;
    private BigDecimal grossSalary;
    private BigDecimal socialInsurance;
    private BigDecimal healthInsurance;
    private BigDecimal unemploymentInsurance;
    private BigDecimal personalIncomeTax;
    private BigDecimal otherDeductions;
    private BigDecimal totalDeductions;
    private BigDecimal netSalary;
    private String paymentStatus;
    private LocalDate paymentDate;
}
