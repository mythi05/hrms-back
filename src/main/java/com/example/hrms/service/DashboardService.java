package com.example.hrms.service;

import com.example.hrms.dto.DashboardStatsDTO;
import com.example.hrms.entity.Payroll;
import com.example.hrms.entity.LeaveRequest;
import com.example.hrms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final PayrollRepository payrollRepository;

    // Admin Dashboard Stats
    public DashboardStatsDTO getAdminStats() {
        LocalDate today = LocalDate.now();
        
        Long totalEmployees = employeeRepository.count();
        Long presentToday = attendanceRepository.countByDateAndCheckInIsNotNull(today);
        Long onLeaveToday = leaveRequestRepository.countByStartDateLessThanEqualAndEndDateGreaterThanEqualAndStatus(today, today, LeaveRequest.Status.APPROVED);
        Long absentToday = totalEmployees - presentToday - onLeaveToday;
        Long pendingLeaveRequests = leaveRequestRepository.countByStatus(LeaveRequest.Status.PENDING);
        
        return DashboardStatsDTO.builder()
                .totalEmployees(totalEmployees)
                .presentToday(presentToday)
                .onLeaveToday(onLeaveToday)
                .absentToday(absentToday)
                .pendingLeaveRequests(pendingLeaveRequests)
                .newApplications(0L) // Can be implemented later
                .build();
    }

    // Employee Dashboard Stats
    public Map<String, Object> getEmployeeStats(Long employeeId) {
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.now();
        
        // Get current month attendance
        List<Object[]> monthlyAttendance = attendanceRepository.getMonthlyAttendanceStats(employeeId, currentMonth.getMonthValue(), currentMonth.getYear());
        
        // Calculate stats
        int totalDays = monthlyAttendance.size();
        int presentDays = (int) monthlyAttendance.stream().filter(att -> att[1] != null).count();
        int lateDays = (int) monthlyAttendance.stream().filter(att -> att[2] != null && att[2].toString().equals("LATE")).count();
        
        // Leave balance
        int totalLeaveDays = 12; // Default annual leave
        Long usedLeaveDaysLong = leaveRequestRepository.getUsedLeaveDays(employeeId, LeaveRequest.Status.APPROVED, currentMonth.getYear());
        int usedLeaveDays = usedLeaveDaysLong != null ? usedLeaveDaysLong.intValue() : 0;
        int remainingLeaveDays = totalLeaveDays - usedLeaveDays;
        
        // Overtime this month
        Double overtimeHours = attendanceRepository.getOvertimeHours(employeeId, currentMonth.getMonthValue(), currentMonth.getYear());
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDays", totalDays);
        stats.put("presentDays", presentDays);
        stats.put("lateDays", lateDays);
        stats.put("remainingLeaveDays", remainingLeaveDays);
        stats.put("totalLeaveDays", totalLeaveDays);
        stats.put("usedLeaveDays", usedLeaveDays);
        stats.put("overtimeHours", overtimeHours != null ? overtimeHours : 0.0);
        
        return stats;
    }

    // Recent Activities for Admin
    public List<Map<String, Object>> getRecentActivities() {
        List<Map<String, Object>> activities = new ArrayList<>();
        
        // Get recent attendance records
        attendanceRepository.findTop10ByOrderByCheckInDesc().forEach(att -> {
            Map<String, Object> activity = new HashMap<>();
            activity.put("type", "attendance");
            activity.put("employeeName", "Employee ID: " + att.getEmployeeId());
            activity.put("action", att.getCheckIn() != null ? "Check in" : "Check out");
            activity.put("time", att.getCheckIn() != null ? att.getCheckIn().toString() : (att.getCheckOut() != null ? att.getCheckOut().toString() : "No time"));
            activity.put("date", att.getDate().toString());
            activities.add(activity);
        });
        
        // Get recent leave requests
        leaveRequestRepository.findTop10ByOrderByCreatedAtDesc().forEach(leave -> {
            Map<String, Object> activity = new HashMap<>();
            activity.put("type", "leave");
            activity.put("employeeName", "Employee ID: " + leave.getEmployeeId());
            activity.put("action", "Leave request");
            activity.put("status", leave.getStatus().toString());
            activity.put("date", leave.getCreatedAt().toString());
            activities.add(activity);
        });
        
        // Sort by date and limit to 10
        activities.sort((a, b) -> ((String) b.get("date")).compareTo((String) a.get("date")));
        return activities.stream().limit(10).toList();
    }

    // Department Distribution
    public List<Map<String, Object>> getDepartmentDistribution() {
        List<Object[]> departmentStats = employeeRepository.getDepartmentDistribution();
        
        return departmentStats.stream().map(stat -> {
            Map<String, Object> dept = new HashMap<>();
            dept.put("name", stat[0]);
            dept.put("count", stat[1]);
            return dept;
        }).toList();
    }

    // Pending Requests
    public List<Map<String, Object>> getPendingRequests() {
        return leaveRequestRepository.findByStatus(LeaveRequest.Status.PENDING).stream().map(leave -> {
            Map<String, Object> request = new HashMap<>();
            request.put("id", leave.getId());
            request.put("employeeName", "Employee ID: " + leave.getEmployeeId());
            request.put("type", "Leave Request");
            request.put("startDate", leave.getStartDate());
            request.put("endDate", leave.getEndDate());
            request.put("reason", leave.getReason());
            request.put("createdAt", leave.getCreatedAt());
            return request;
        }).toList();
    }

    // Birthdays this month
    public List<Map<String, Object>> getBirthdaysThisMonth() {
        LocalDate today = LocalDate.now();
        int currentMonth = today.getMonthValue();
        
        return employeeRepository.findByMonthOfBirth(currentMonth).stream().map(emp -> {
            Map<String, Object> birthday = new HashMap<>();
            birthday.put("id", emp.getId());
            birthday.put("name", emp.getFullName());
            birthday.put("department", emp.getDepartment());
            birthday.put("birthday", emp.getDob().getDayOfMonth() + "/" + emp.getDob().getMonthValue());
            return birthday;
        }).toList();
    }

    // Attendance statistics for chart
    public List<Map<String, Object>> getAttendanceStats() {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(6); // Last 7 days
        
        List<Map<String, Object>> stats = new ArrayList<>();
        
        for (LocalDate date = weekStart; !date.isAfter(today); date = date.plusDays(1)) {
            Long present = attendanceRepository.countByDateAndCheckInIsNotNull(date);
            Long totalEmployees = employeeRepository.count();
            Long absent = totalEmployees - present;
            
            Map<String, Object> dayStat = new HashMap<>();
            dayStat.put("date", date.getDayOfMonth() + "/" + date.getMonthValue());
            dayStat.put("present", present != null ? present : 0);
            dayStat.put("absent", absent != null ? absent : 0);
            stats.add(dayStat);
        }
        
        return stats;
    }

    // Payroll trends for chart
    public List<Map<String, Object>> getPayrollTrends() {
        YearMonth currentMonth = YearMonth.now();
        List<Map<String, Object>> trends = new ArrayList<>();
        
        // Get last 6 months data
        for (int i = 5; i >= 0; i--) {
            YearMonth month = currentMonth.minusMonths(i);
            List<Object[]> payrollStats = payrollRepository.getMonthlyPayrollStats(month.getMonthValue(), month.getYear());
            
            Map<String, Object> monthData = new HashMap<>();
            monthData.put("month", month.getMonthValue() + "/" + month.getYear());
            
            if (!payrollStats.isEmpty()) {
                Object[] stats = payrollStats.get(0);
                monthData.put("totalPayroll", stats[0] != null ? stats[0] : 0);
                monthData.put("avgSalary", stats[1] != null ? stats[1] : 0);
                monthData.put("employeeCount", stats[2] != null ? stats[2] : 0);
            } else {
                monthData.put("totalPayroll", 0);
                monthData.put("avgSalary", 0);
                monthData.put("employeeCount", 0);
            }
            
            trends.add(monthData);
        }
        
        return trends;
    }

    // Employee payroll trends
    public List<Map<String, Object>> getEmployeePayrollTrends(Long employeeId) {
        YearMonth currentMonth = YearMonth.now();
        List<Map<String, Object>> trends = new ArrayList<>();
        
        // Get last 6 months data
        for (int i = 5; i >= 0; i--) {
            YearMonth month = currentMonth.minusMonths(i);
            Optional<Payroll> payrollOpt = payrollRepository.findByEmployeeIdAndMonthAndYear(employeeId, month.getMonthValue(), month.getYear());
            
            Map<String, Object> monthData = new HashMap<>();
            monthData.put("month", month.getMonthValue() + "/" + month.getYear());
            
            if (payrollOpt.isPresent()) {
                Payroll payroll = payrollOpt.get();
                monthData.put("netSalary", payroll.getNetSalary());
                monthData.put("basicSalary", payroll.getBasicSalary());
                monthData.put("bonus", payroll.getBonus());
            } else {
                monthData.put("netSalary", 0);
                monthData.put("basicSalary", 0);
                monthData.put("bonus", 0);
            }
            
            trends.add(monthData);
        }
        
        return trends;
    }
}
