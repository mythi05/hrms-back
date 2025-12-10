package com.example.hrms.mapper;

import com.example.hrms.dto.LeaveRequestDTO;
import com.example.hrms.entity.LeaveRequest;

public class LeaveRequestMapper {

    public static LeaveRequestDTO toDTO(LeaveRequest entity) {
        if (entity == null) return null;

        LeaveRequestDTO dto = new LeaveRequestDTO();
        dto.setId(entity.getId());
        dto.setEmployeeId(entity.getEmployeeId());
        dto.setLeaveType(entity.getLeaveType().name());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setDaysCount(entity.getDaysCount());
        dto.setReason(entity.getReason());
        dto.setStatus(entity.getStatus().name());
        dto.setApproverId(entity.getApproverId());
        dto.setApprovedAt(entity.getApprovedAt());
        dto.setRejectReason(entity.getRejectReason());
        // employeeName / approverName có thể được set thêm nếu cần join Employee
        return dto;
    }

    public static LeaveRequest toEntity(LeaveRequestDTO dto) {
        if (dto == null) return null;

        return LeaveRequest.builder()
                .id(dto.getId())
                .employeeId(dto.getEmployeeId())
                .leaveType(LeaveRequest.LeaveType.valueOf(dto.getLeaveType().toUpperCase()))
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .daysCount(dto.getDaysCount())
                .reason(dto.getReason())
                .status(dto.getStatus() != null
                        ? LeaveRequest.Status.valueOf(dto.getStatus().toUpperCase())
                        : LeaveRequest.Status.PENDING)
                .approverId(dto.getApproverId())
                .approvedAt(dto.getApprovedAt())
                .rejectReason(dto.getRejectReason())
                .build();
    }
}
