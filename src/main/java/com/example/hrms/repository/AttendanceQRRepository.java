package com.example.hrms.repository;

import com.example.hrms.entity.AttendanceQR;
import com.example.hrms.entity.QRScanLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceQRRepository extends JpaRepository<AttendanceQR, Long> {

    Optional<AttendanceQR> findByQrCode(String qrCode);

    List<AttendanceQR> findByDate(LocalDate date);

    List<AttendanceQR> findByDateAndShiftType(LocalDate date, AttendanceQR.ShiftType shiftType);

    List<AttendanceQR> findByDateAndShiftTypeAndScanType(LocalDate date, AttendanceQR.ShiftType shiftType, QRScanLog.ScanType scanType);

    List<AttendanceQR> findByDateAndIsActive(LocalDate date, Boolean isActive);

    @Query("SELECT qr FROM AttendanceQR qr WHERE qr.date = :date " +
           "AND qr.shiftType = :shiftType " +
           "AND qr.scanType = :scanType " +
           "AND qr.isActive = true " +
           "AND :currentTime BETWEEN qr.validFrom AND qr.validTo")
    Optional<AttendanceQR> findValidQR(LocalDate date, AttendanceQR.ShiftType shiftType, QRScanLog.ScanType scanType, LocalTime currentTime);

    @Query("SELECT qr FROM AttendanceQR qr WHERE qr.date = :date " +
           "AND qr.isActive = true " +
           "AND :currentTime BETWEEN qr.validFrom AND qr.validTo")
    List<AttendanceQR> findValidQRsByTime(LocalDate date, LocalTime currentTime);

    List<AttendanceQR> findByCreatedBy(Long createdBy);

    @Query("SELECT qr FROM AttendanceQR qr WHERE qr.date BETWEEN :startDate AND :endDate " +
           "ORDER BY qr.date DESC, qr.shiftType ASC")
    List<AttendanceQR> findByDateBetween(LocalDate startDate, LocalDate endDate);
}
