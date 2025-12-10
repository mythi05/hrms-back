package com.example.hrms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class LeaveRequestDTO {
    
    private Long id;
    private Long employeeId;
    private String employeeName;
    
    @NotBlank(message = "Loại nghỉ phép không được để trống")
    private String leaveType;
    
    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate startDate;
    
    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDate endDate;
    
    private BigDecimal daysCount;
    
    @NotBlank(message = "Lý do không được để trống")
    private String reason;
    
    private String status;
    private Long approverId;
    private String approverName;
    private LocalDateTime approvedAt;
    private String rejectReason;
}
