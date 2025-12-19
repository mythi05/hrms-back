package com.example.hrms.service.impl;

import com.example.hrms.dto.AttendanceDTO;
import com.example.hrms.entity.Attendance;
import com.example.hrms.entity.Employee;
import com.example.hrms.mapper.AttendanceMapper;
import com.example.hrms.repository.AttendanceRepository;
import com.example.hrms.repository.EmployeeRepository;
import com.example.hrms.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepo;
    private final EmployeeRepository employeeRepo;

    @Override
    public AttendanceDTO checkIn(Long employeeId) {
        LocalDate today = LocalDate.now();

        attendanceRepo.findByEmployeeIdAndDate(employeeId, today)
                .ifPresent(a -> { throw new RuntimeException("Hôm nay bạn đã check-in rồi!"); });

        Attendance attendance = Attendance.builder()
                .employeeId(employeeId)
                .date(today)
                .checkIn(LocalTime.now())
                .status(LocalTime.now().isAfter(LocalTime.of(8, 0)) ?
                        Attendance.Status.LATE : Attendance.Status.PRESENT)
                .build();

        Attendance saved = attendanceRepo.save(attendance);
        return AttendanceMapper.toDTO(saved);
    }

    @Override
    public AttendanceDTO checkOut(Long employeeId) {
        LocalDate today = LocalDate.now();

        Attendance attendance = attendanceRepo.findByEmployeeIdAndDate(employeeId, today)
                .orElseThrow(() -> new RuntimeException("Bạn chưa check-in!"));

        if (attendance.getCheckOut() != null) {
            throw new RuntimeException("Bạn đã check-out rồi!");
        }

        attendance.setCheckOut(LocalTime.now());

        Duration duration = Duration.between(attendance.getCheckIn(), attendance.getCheckOut());
        double hours = duration.toMinutes() / 60.0;
        attendance.setTotalHours(BigDecimal.valueOf(hours).setScale(2, RoundingMode.HALF_UP));

        attendanceRepo.save(attendance);

        return AttendanceMapper.toDTO(attendance);
    }

    @Override
    public AttendanceDTO getTodayAttendance(Long employeeId) {
        return attendanceRepo.findByEmployeeIdAndDate(employeeId, LocalDate.now())
                .map(AttendanceMapper::toDTO)
                .orElse(null);
    }

    @Override
    public List<AttendanceDTO> getAttendanceHistory(Long employeeId) {
        return attendanceRepo.findByEmployeeId(employeeId)
                .stream()
                .map(AttendanceMapper::toDTO)
                .toList();
    }

    @Override
    public List<AttendanceDTO> getAttendanceOfMonth(Long employeeId, int month, int year) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        return attendanceRepo.findByEmployeeIdAndDateBetween(employeeId, startDate, endDate)
                .stream()
                .map(AttendanceMapper::toDTO)
                .toList();
    }

    @Override
    public List<AttendanceDTO> getAttendanceByDate(LocalDate date) {
        return attendanceRepo.findByDate(date)
                .stream()
                .map(AttendanceMapper::toDTO)
                .toList();
    }

    @Override
    public List<AttendanceDTO> adminFindAll() {
        return attendanceRepo.findAll()
                .stream()
                .map(this::toDTOWithEmployeeInfo)
                .toList();
    }

    @Override
    public AttendanceDTO adminCreate(AttendanceDTO dto) {
        Attendance entity = AttendanceMapper.toEntity(dto);
        return AttendanceMapper.toDTO(attendanceRepo.save(entity));
    }

    @Override
    public AttendanceDTO adminUpdate(Long id, AttendanceDTO dto) {
        Attendance entity = attendanceRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Attendance not found"));

        entity.setCheckIn(dto.getCheckIn());
        entity.setCheckOut(dto.getCheckOut());
        if (dto.getStatus() != null) {
            entity.setStatus(Attendance.Status.valueOf(dto.getStatus()));
        }
        entity.setNote(dto.getNote());

        return AttendanceMapper.toDTO(attendanceRepo.save(entity));
    }

    @Override
    public void adminDelete(Long id) {
        attendanceRepo.deleteById(id);
    }

    @Override
    public void adminDeleteAll() {
        attendanceRepo.deleteAll();
    }

    @Override
    public List<AttendanceDTO> getAttendanceOfMonthForAll(int month, int year) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        return attendanceRepo.findByDateBetween(startDate, endDate)
                .stream()
                .map(this::toDTOWithEmployeeInfo)
                .toList();
    }

    // Helper method để populate thông tin nhân viên vào DTO
    private AttendanceDTO toDTOWithEmployeeInfo(Attendance entity) {
        AttendanceDTO dto = AttendanceMapper.toDTO(entity);
        
        // Lấy thông tin nhân viên
        Employee employee = employeeRepo.findById(entity.getEmployeeId()).orElse(null);
        if (employee != null) {
            dto.setEmployeeName(employee.getFullName());
            dto.setDepartment(employee.getDepartment());
        } else {
            dto.setEmployeeName("Unknown");
            dto.setDepartment("-");
        }
        
        return dto;
    }
}
