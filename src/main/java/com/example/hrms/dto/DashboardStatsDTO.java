package com.example.hrms.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {
    
    private Long totalEmployees;
    private Long presentToday;
    private Long onLeaveToday;
    private Long absentToday;
    private Long pendingLeaveRequests;
    private Long newApplications;
}
