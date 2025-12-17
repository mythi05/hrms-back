package com.example.hrms.service.impl;

import com.example.hrms.entity.AttendanceQR;
import com.example.hrms.entity.QRScanLog;
import com.example.hrms.repository.AttendanceQRRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class QRAttendanceDailyScheduler {

    private final AttendanceQRRepository attendanceQRRepository;

    @Scheduled(cron = "0 1 0 * * *")
    public void ensureTodayQRCodes() {
        LocalDate today = LocalDate.now();
        ensureOne(today, QRScanLog.ScanType.CHECK_IN);
        ensureOne(today, QRScanLog.ScanType.CHECK_OUT);
    }

    private void ensureOne(LocalDate date, QRScanLog.ScanType scanType) {
        boolean exists = !attendanceQRRepository
                .findByDateAndShiftTypeAndScanType(date, AttendanceQR.ShiftType.FULL_TIME, scanType)
                .isEmpty();
        if (exists) {
            return;
        }

        AttendanceQR qr = AttendanceQR.builder()
                .qrCode(UUID.randomUUID().toString())
                .date(date)
                .shiftType(AttendanceQR.ShiftType.FULL_TIME)
                .scanType(scanType)
                .validFrom(LocalTime.of(0, 0))
                .validTo(LocalTime.of(23, 59))
                .isActive(true)
                .createdBy(0L)
                .build();

        attendanceQRRepository.save(qr);
    }
}
