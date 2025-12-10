package com.example.hrms.mapper;

import com.example.hrms.dto.AttendanceDTO;
import com.example.hrms.entity.Attendance;

public class AttendanceMapper {

    public static AttendanceDTO toDTO(Attendance entity) {
        if (entity == null) return null;

        AttendanceDTO dto = new AttendanceDTO();
        dto.setId(entity.getId());
        dto.setEmployeeId(entity.getEmployeeId());
        dto.setDate(entity.getDate());
        dto.setCheckIn(entity.getCheckIn());
        dto.setCheckOut(entity.getCheckOut());
        dto.setTotalHours(entity.getTotalHours());
        dto.setStatus(entity.getStatus().name());
        dto.setNote(entity.getNote());

        return dto;
    }

    public static Attendance toEntity(AttendanceDTO dto) {
        if (dto == null) return null;

        return Attendance.builder()
                .id(dto.getId())
                .employeeId(dto.getEmployeeId())
                .date(dto.getDate())
                .checkIn(dto.getCheckIn())
                .checkOut(dto.getCheckOut())
                .totalHours(dto.getTotalHours())
                .status(Attendance.Status.valueOf(dto.getStatus()))
                .note(dto.getNote())
                .build();
    }
}
