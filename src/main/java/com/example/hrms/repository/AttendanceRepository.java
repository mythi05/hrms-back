package com.example.hrms.repository;

import com.example.hrms.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    // Lấy attendance của 1 nhân viên theo ngày
    Optional<Attendance> findByEmployeeIdAndDate(Long employeeId, LocalDate date);

    // Lấy tất cả attendance của 1 nhân viên trong khoảng thời gian
    List<Attendance> findByEmployeeIdAndDateBetween(Long employeeId, LocalDate startDate, LocalDate endDate);

    // Lấy tất cả attendance theo employeeId
    List<Attendance> findByEmployeeId(Long employeeId);

    // Lấy tất cả attendance theo ngày
    List<Attendance> findByDate(LocalDate date);

    // Lấy tất cả attendance trong khoảng thời gian
    List<Attendance> findByDateBetween(LocalDate startDate, LocalDate endDate);

    // Lấy tất cả attendance có checkOut = null
    List<Attendance> findByDateAndCheckOutIsNull(LocalDate date);
    
    // Dashboard methods
    Long countByDateAndCheckInIsNotNull(LocalDate date);
    
    @Query("SELECT a.date, a.checkIn, a.status FROM Attendance a WHERE a.employeeId = :employeeId " +
           "AND MONTH(a.date) = :month AND YEAR(a.date) = :year")
    List<Object[]> getMonthlyAttendanceStats(Long employeeId, int month, int year);
    
    @Query("SELECT SUM(a.totalHours) FROM Attendance a WHERE a.employeeId = :employeeId " +
           "AND MONTH(a.date) = :month AND YEAR(a.date) = :year AND a.totalHours > 8")
    Double getOvertimeHours(Long employeeId, int month, int year);
    
    List<Attendance> findTop10ByOrderByCheckInDesc();
}
