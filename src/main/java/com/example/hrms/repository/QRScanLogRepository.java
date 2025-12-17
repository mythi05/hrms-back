package com.example.hrms.repository;

import com.example.hrms.entity.QRScanLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface QRScanLogRepository extends JpaRepository<QRScanLog, Long> {

    List<QRScanLog> findByEmployeeId(Long employeeId);

    List<QRScanLog> findByQrCode(String qrCode);

    List<QRScanLog> findByScanTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    List<QRScanLog> findByEmployeeIdAndScanTimeBetween(Long employeeId, LocalDateTime startTime, LocalDateTime endTime);

    List<QRScanLog> findByStatusAndScanTimeBetween(QRScanLog.ScanStatus status, LocalDateTime startTime, LocalDateTime endTime);

    Long countByStatusAndScanTimeBetween(QRScanLog.ScanStatus status, LocalDateTime startTime, LocalDateTime endTime);

    boolean existsByEmployeeIdAndQrCodeAndScanTypeAndScanTimeBetween(
            Long employeeId,
            String qrCode,
            QRScanLog.ScanType scanType,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    @Query("SELECT log FROM QRScanLog log WHERE log.scanTime >= :startTime " +
           "ORDER BY log.scanTime DESC")
    List<QRScanLog> findRecentScans(LocalDateTime startTime);

    List<QRScanLog> findTop50ByOrderByScanTimeDesc();
}
