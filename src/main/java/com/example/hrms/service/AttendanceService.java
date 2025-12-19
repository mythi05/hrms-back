package com.example.hrms.service;

import com.example.hrms.dto.AttendanceDTO;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceService {

    AttendanceDTO checkIn(Long employeeId);

    AttendanceDTO checkOut(Long employeeId);

    AttendanceDTO getTodayAttendance(Long employeeId);

    List<AttendanceDTO> getAttendanceHistory(Long employeeId);

    List<AttendanceDTO> getAttendanceOfMonth(Long employeeId, int month, int year);

    List<AttendanceDTO> getAttendanceByDate(LocalDate date);

    List<AttendanceDTO> adminFindAll();

    AttendanceDTO adminCreate(AttendanceDTO dto);

    AttendanceDTO adminUpdate(Long id, AttendanceDTO dto);

    void adminDelete(Long id);

    void adminDeleteAll();

    List<AttendanceDTO> getAttendanceOfMonthForAll(int month, int year);
}
