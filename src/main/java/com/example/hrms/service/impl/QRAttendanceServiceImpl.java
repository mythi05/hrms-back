package com.example.hrms.service.impl;

import com.example.hrms.dto.AttendanceDashboardDTO;
import com.example.hrms.dto.AttendanceQRDTO;
import com.example.hrms.dto.CreateQRDTO;
import com.example.hrms.dto.QRScanRequestDTO;
import com.example.hrms.dto.QRScanResponseDTO;
import com.example.hrms.entity.Attendance;
import com.example.hrms.entity.AttendanceQR;
import com.example.hrms.entity.Employee;
import com.example.hrms.entity.QRScanLog;
import com.example.hrms.repository.AttendanceQRRepository;
import com.example.hrms.repository.AttendanceRepository;
import com.example.hrms.repository.EmployeeRepository;
import com.example.hrms.repository.QRScanLogRepository;
import com.example.hrms.service.QRAttendanceService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QRAttendanceServiceImpl implements QRAttendanceService {

    private final AttendanceQRRepository attendanceQRRepository;
    private final QRScanLogRepository qrScanLogRepository;
    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;

    private static final Map<AttendanceQR.ShiftType, LocalTime> SHIFT_START_TIME = Map.of(
            AttendanceQR.ShiftType.FULL_TIME, LocalTime.of(8, 0),
            AttendanceQR.ShiftType.MORNING, LocalTime.of(8, 0),
            AttendanceQR.ShiftType.AFTERNOON, LocalTime.of(13, 0),
            AttendanceQR.ShiftType.NIGHT, LocalTime.of(20, 0)
    );

    @Override
    public AttendanceQRDTO createQR(CreateQRDTO dto, Long createdBy) {
        if (dto.getValidFrom() == null || dto.getValidTo() == null || !dto.getValidFrom().isBefore(dto.getValidTo())) {
            throw new RuntimeException("Thời gian hiệu lực không hợp lệ (validFrom phải nhỏ hơn validTo)");
        }
        if (dto.getScanType() == null) {
            throw new RuntimeException("Loại quét không được để trống");
        }

        String qrCode = UUID.randomUUID().toString();

        AttendanceQR qr = AttendanceQR.builder()
                .qrCode(qrCode)
                .date(dto.getDate())
                .shiftType(dto.getShiftType())
                .scanType(dto.getScanType())
                .validFrom(dto.getValidFrom())
                .validTo(dto.getValidTo())
                .isActive(true)
                .createdBy(createdBy)
                .build();

        AttendanceQR saved = attendanceQRRepository.save(qr);
        return toDTO(saved, LocalTime.now());
    }

    @Override
    public AttendanceQRDTO getQRById(Long id) {
        AttendanceQR qr = attendanceQRRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy QR"));
        return toDTO(qr, LocalTime.now());
    }

    @Override
    public List<AttendanceQRDTO> getQRsByDate(LocalDate date) {
        LocalTime now = LocalTime.now();
        return attendanceQRRepository.findByDate(date).stream()
                .sorted(Comparator.comparing(AttendanceQR::getShiftType).thenComparing(AttendanceQR::getScanType))
                .map(qr -> toDTO(qr, now))
                .toList();
    }

    @Override
    public List<AttendanceQRDTO> getActiveQRs(LocalDate date, LocalTime currentTime) {
        LocalTime t = currentTime != null ? currentTime : LocalTime.now();
        return attendanceQRRepository.findValidQRsByTime(date, t).stream()
                .sorted(Comparator.comparing(AttendanceQR::getShiftType).thenComparing(AttendanceQR::getScanType))
                .map(qr -> toDTO(qr, t))
                .toList();
    }

    @Override
    public void deactivateQR(Long id) {
        AttendanceQR qr = attendanceQRRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy QR"));
        qr.setIsActive(false);
        attendanceQRRepository.save(qr);
    }

    @Override
    public byte[] generateQRImage(String qrCode, int width, int height) {
        try {
            int w = width > 0 ? width : 300;
            int h = height > 0 ? height : 300;

            BitMatrix matrix = new MultiFormatWriter().encode(qrCode, BarcodeFormat.QR_CODE, w, h);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Không thể tạo ảnh QR: " + e.getMessage());
        }
    }

    @Override
    public QRScanResponseDTO scanQR(QRScanRequestDTO request, Long employeeId) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        LocalTime nowTime = now.toLocalTime();

        if (employeeId == null) {
            return QRScanResponseDTO.error(QRScanLog.ScanStatus.EMPLOYEE_NOT_FOUND, "Không xác định được nhân viên. Vui lòng đăng nhập lại.");
        }

        Optional<Employee> employeeOpt = employeeRepository.findById(employeeId);
        if (employeeOpt.isEmpty()) {
            logScanFail(request, employeeId, null, QRScanLog.ScanStatus.EMPLOYEE_NOT_FOUND, "Không tìm thấy nhân viên");
            return QRScanResponseDTO.error(QRScanLog.ScanStatus.EMPLOYEE_NOT_FOUND, "Không tìm thấy nhân viên.");
        }

        Employee employee = employeeOpt.get();

        AttendanceQR qr = attendanceQRRepository.findByQrCode(request.getQrCode())
                .orElse(null);

        if (qr == null) {
            logScanFail(request, employeeId, null, QRScanLog.ScanStatus.QR_NOT_FOUND, "QR không tồn tại");
            return QRScanResponseDTO.error(QRScanLog.ScanStatus.QR_NOT_FOUND, "QR không hợp lệ.");
        }

        if (qr.getIsActive() == null || !qr.getIsActive() || !today.equals(qr.getDate())) {
            logScanFail(request, employeeId, qr, QRScanLog.ScanStatus.QR_EXPIRED, "QR không còn hiệu lực hoặc sai ngày");
            QRScanResponseDTO resp = QRScanResponseDTO.error(QRScanLog.ScanStatus.QR_EXPIRED, "QR không còn hiệu lực hoặc không đúng ngày.");
            resp.setScanType(qr.getScanType());
            resp.setQrCode(qr.getQrCode());
            resp.setShiftType(shiftDisplay(qr.getShiftType()));
            resp.setValidTimeRange(timeRange(qr));
            return resp;
        }

        QRScanLog.ScanType scanType = request.getScanType();
        if (scanType == null) {
            Attendance todayAttendance = attendanceRepository.findByEmployeeIdAndDate(employeeId, today).orElse(null);
            if (todayAttendance == null) {
                scanType = QRScanLog.ScanType.CHECK_IN;
            } else if (todayAttendance.getCheckOut() == null) {
                scanType = QRScanLog.ScanType.CHECK_OUT;
            } else {
                logScanFail(request, employeeId, qr, QRScanLog.ScanStatus.ALREADY_SCANNED, "Đã check-in và check-out hôm nay");
                return QRScanResponseDTO.error(QRScanLog.ScanStatus.ALREADY_SCANNED, "Hôm nay bạn đã check-in và check-out rồi.");
            }
        }

        if (qr.getScanType() != scanType) {
            logScanFail(request, employeeId, qr, QRScanLog.ScanStatus.INVALID_SHIFT, "Quét sai loại QR");
            String expected = qr.getScanType() == QRScanLog.ScanType.CHECK_IN ? "vào ca" : "ra ca";
            return QRScanResponseDTO.error(QRScanLog.ScanStatus.INVALID_SHIFT, "Quét sai loại QR. Vui lòng quét QR " + expected + ".");
        }

        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay().minusNanos(1);
        if (qrScanLogRepository.existsByEmployeeIdAndQrCodeAndScanTypeAndScanTimeBetween(employeeId, qr.getQrCode(), scanType, startOfDay, endOfDay)) {
            logScanFail(request, employeeId, qr, QRScanLog.ScanStatus.ALREADY_SCANNED, "Đã quét QR này hôm nay");
            return QRScanResponseDTO.error(QRScanLog.ScanStatus.ALREADY_SCANNED, "Mỗi ca chỉ quét 1 lần. Bạn đã chấm công ca này rồi.");
        }

        if (scanType == QRScanLog.ScanType.CHECK_IN) {
            if (attendanceRepository.findByEmployeeIdAndDate(employeeId, today).isPresent()) {
                logScanFail(request, employeeId, qr, QRScanLog.ScanStatus.ALREADY_SCANNED, "Đã check-in hôm nay");
                return QRScanResponseDTO.error(QRScanLog.ScanStatus.ALREADY_SCANNED, "Hôm nay bạn đã check-in rồi!");
            }

            LocalTime expectedStart = SHIFT_START_TIME.getOrDefault(qr.getShiftType(), LocalTime.of(8, 0));
            boolean isLate = nowTime.isAfter(expectedStart);
            Attendance attendance = Attendance.builder()
                    .employeeId(employeeId)
                    .date(today)
                    .checkIn(nowTime)
                    .status(isLate ? Attendance.Status.LATE : Attendance.Status.PRESENT)
                    .build();

            Attendance saved = attendanceRepository.save(attendance);
            logScanSuccess(request, employeeId, qr, scanType);

            String msg = isLate ? "Check-in thành công (Đi muộn)" : "Check-in thành công";
            QRScanResponseDTO resp = QRScanResponseDTO.success(msg, nowTime, employee.getFullName());
            resp.setScanType(scanType);
            resp.setAttendanceId(saved.getId());
            resp.setEmployeeId(String.valueOf(employee.getId()));
            resp.setQrCode(qr.getQrCode());
            resp.setShiftType(shiftDisplay(qr.getShiftType()));
            resp.setValidTimeRange(timeRange(qr));
            return resp;
        }

        Attendance attendance = attendanceRepository.findByEmployeeIdAndDate(employeeId, today)
                .orElse(null);
        if (attendance == null) {
            logScanFail(request, employeeId, qr, QRScanLog.ScanStatus.INVALID_SHIFT, "Chưa check-in");
            return QRScanResponseDTO.error(QRScanLog.ScanStatus.INVALID_SHIFT, "Bạn chưa check-in! Vui lòng quét QR vào ca trước.");
        }
        if (attendance.getCheckOut() != null) {
            logScanFail(request, employeeId, qr, QRScanLog.ScanStatus.ALREADY_SCANNED, "Đã check-out");
            return QRScanResponseDTO.error(QRScanLog.ScanStatus.ALREADY_SCANNED, "Bạn đã check-out rồi!");
        }

        attendance.setCheckOut(nowTime);
        Duration duration = Duration.between(attendance.getCheckIn(), attendance.getCheckOut());
        double hours = duration.toMinutes() / 60.0;
        attendance.setTotalHours(BigDecimal.valueOf(hours).setScale(2, RoundingMode.HALF_UP));
        attendanceRepository.save(attendance);
        logScanSuccess(request, employeeId, qr, scanType);

        QRScanResponseDTO resp = QRScanResponseDTO.success("Check-out thành công", nowTime, employee.getFullName());
        resp.setScanType(scanType);
        resp.setAttendanceId(attendance.getId());
        resp.setEmployeeId(String.valueOf(employee.getId()));
        resp.setQrCode(qr.getQrCode());
        resp.setShiftType(shiftDisplay(qr.getShiftType()));
        resp.setValidTimeRange(timeRange(qr));
        return resp;
    }

    @Override
    public AttendanceDashboardDTO getDashboardData(LocalDate date) {
        LocalDate d = date != null ? date : LocalDate.now();
        LocalDateTime start = d.atStartOfDay();
        LocalDateTime end = d.plusDays(1).atStartOfDay().minusNanos(1);

        AttendanceDashboardDTO dto = new AttendanceDashboardDTO();
        dto.setDate(d);

        List<AttendanceQR> qrs = attendanceQRRepository.findByDate(d);
        dto.setTotalQRs(qrs.size());
        dto.setActiveQRs(qrs.stream().filter(q -> Boolean.TRUE.equals(q.getIsActive())).count());

        List<QRScanLog> scans = qrScanLogRepository.findByScanTimeBetween(start, end);
        dto.setTotalScans(scans.size());

        long successful = qrScanLogRepository.countByStatusAndScanTimeBetween(QRScanLog.ScanStatus.SUCCESS, start, end);
        dto.setSuccessfulScans(successful);
        dto.setFailedScans(Math.max(0, scans.size() - successful));

        long totalEmployees = employeeRepository.count();
        dto.setTotalEmployees(totalEmployees);
        long checkedIn = Optional.ofNullable(attendanceRepository.countByDateAndCheckInIsNotNull(d)).orElse(0L);
        dto.setCheckedInEmployees(checkedIn);
        dto.setPendingEmployees(Math.max(0, totalEmployees - checkedIn));

        long late = attendanceRepository.findByDate(d).stream().filter(a -> a.getStatus() == Attendance.Status.LATE).count();
        dto.setLateEmployees(late);

        dto.setActiveQRsList(getActiveQRs(d, LocalTime.now()));
        return dto;
    }

    @Override
    public List<QRScanLog.QRScanLogDTO> getRecentScans(int limit) {
        int l = limit > 0 ? limit : 20;
        return qrScanLogRepository.findTop50ByOrderByScanTimeDesc().stream()
                .limit(l)
                .map(this::toScanDTO)
                .toList();
    }

    @Override
    public List<QRScanLog.QRScanLogDTO> getScanHistory(LocalDate date, QRScanLog.ScanStatus status) {
        LocalDate d = date != null ? date : LocalDate.now();
        LocalDateTime start = d.atStartOfDay();
        LocalDateTime end = d.plusDays(1).atStartOfDay().minusNanos(1);

        List<QRScanLog> logs = status == null
                ? qrScanLogRepository.findByScanTimeBetween(start, end)
                : qrScanLogRepository.findByStatusAndScanTimeBetween(status, start, end);

        return logs.stream()
                .sorted(Comparator.comparing(QRScanLog::getScanTime).reversed())
                .map(this::toScanDTO)
                .toList();
    }

    @Override
    public byte[] exportAttendanceData(LocalDate startDate, LocalDate endDate, String format) {
        LocalDate start = startDate != null ? startDate : LocalDate.now().withDayOfMonth(1);
        LocalDate end = endDate != null ? endDate : LocalDate.now();

        List<Attendance> list = attendanceRepository.findByDateBetween(start, end);

        StringBuilder sb = new StringBuilder();
        sb.append("employeeId,date,checkIn,checkOut,totalHours,status\n");
        for (Attendance a : list) {
            sb.append(a.getEmployeeId()).append(',')
                    .append(a.getDate()).append(',')
                    .append(a.getCheckIn() != null ? a.getCheckIn() : "").append(',')
                    .append(a.getCheckOut() != null ? a.getCheckOut() : "").append(',')
                    .append(a.getTotalHours() != null ? a.getTotalHours() : "").append(',')
                    .append(a.getStatus() != null ? a.getStatus().name() : "")
                    .append('\n');
        }
        return sb.toString().getBytes();
    }

    @Override
    public boolean isValidQR(String qrCode, AttendanceQR.ShiftType shiftType, LocalTime currentTime) {
        LocalTime t = currentTime != null ? currentTime : LocalTime.now();
        LocalDate d = LocalDate.now();
        return attendanceQRRepository.findValidQR(d, shiftType, QRScanLog.ScanType.CHECK_IN, t).isPresent()
                || attendanceQRRepository.findValidQR(d, shiftType, QRScanLog.ScanType.CHECK_OUT, t).isPresent();
    }

    @Override
    public boolean hasEmployeeScannedToday(Long employeeId, String qrCode, QRScanLog.ScanType scanType) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay().minusNanos(1);
        return qrScanLogRepository.existsByEmployeeIdAndQrCodeAndScanTypeAndScanTimeBetween(employeeId, qrCode, scanType, startOfDay, endOfDay);
    }

    private AttendanceQRDTO toDTO(AttendanceQR qr, LocalTime currentTime) {
        AttendanceQRDTO dto = new AttendanceQRDTO();
        dto.setId(qr.getId());
        dto.setQrCode(qr.getQrCode());
        dto.setDate(qr.getDate());
        dto.setShiftType(qr.getShiftType());
        dto.setScanType(qr.getScanType());
        dto.setValidFrom(qr.getValidFrom());
        dto.setValidTo(qr.getValidTo());
        dto.setIsActive(qr.getIsActive());
        dto.setCreatedBy(qr.getCreatedBy());
        dto.setCreatedAt(qr.getCreatedAt());
        dto.setUpdatedAt(qr.getUpdatedAt());

        dto.setShiftTypeDisplay(shiftDisplay(qr.getShiftType()));
        dto.setScanTypeDisplay(scanTypeDisplay(qr.getScanType()));
        dto.setTimeRange(timeRange(qr));

        boolean timeValid = true;
        if (qr.getShiftType() != AttendanceQR.ShiftType.FULL_TIME) {
            timeValid = currentTime != null && !currentTime.isBefore(qr.getValidFrom()) && !currentTime.isAfter(qr.getValidTo());
        }
        dto.setValid(Boolean.TRUE.equals(qr.getIsActive()) && LocalDate.now().equals(qr.getDate()) && timeValid);

        employeeRepository.findById(qr.getCreatedBy()).ifPresent(e -> dto.setCreatedByName(e.getFullName()));
        return dto;
    }

    private String shiftDisplay(AttendanceQR.ShiftType shiftType) {
        if (shiftType == null) return "";
        return switch (shiftType) {
            case FULL_TIME -> "Full-time";
            case MORNING -> "Ca sáng";
            case AFTERNOON -> "Ca chiều";
            case NIGHT -> "Ca tối";
        };
    }

    private String scanTypeDisplay(QRScanLog.ScanType scanType) {
        if (scanType == null) return "";
        return scanType == QRScanLog.ScanType.CHECK_IN ? "Vào ca" : "Ra ca";
    }

    private String timeRange(AttendanceQR qr) {
        return String.format("%s-%s", qr.getValidFrom(), qr.getValidTo());
    }

    private void logScanSuccess(QRScanRequestDTO request, Long employeeId, AttendanceQR qr, QRScanLog.ScanType scanType) {
        QRScanLog log = QRScanLog.builder()
                .qrCode(qr.getQrCode())
                .employeeId(employeeId)
                .scanType(scanType)
                .shiftType(qr.getShiftType())
                .scanTime(LocalDateTime.now())
                .status(QRScanLog.ScanStatus.SUCCESS)
                .ipAddress(request.getIpAddress())
                .userAgent(request.getUserAgent())
                .build();
        qrScanLogRepository.save(log);
    }

    private void logScanFail(QRScanRequestDTO request, Long employeeId, AttendanceQR qr, QRScanLog.ScanStatus status, String message) {
        QRScanLog.ScanType scanType = null;
        if (request != null && request.getScanType() != null) {
            scanType = request.getScanType();
        } else if (qr != null && qr.getScanType() != null) {
            scanType = qr.getScanType();
        } else {
            scanType = QRScanLog.ScanType.CHECK_IN;
        }

        AttendanceQR.ShiftType shiftType = qr != null && qr.getShiftType() != null
                ? qr.getShiftType()
                : AttendanceQR.ShiftType.FULL_TIME;

        QRScanLog log = QRScanLog.builder()
                .qrCode(qr != null ? qr.getQrCode() : (request != null ? request.getQrCode() : ""))
                .employeeId(employeeId)
                .scanType(scanType)
                .shiftType(shiftType)
                .scanTime(LocalDateTime.now())
                .status(status)
                .errorMessage(message)
                .ipAddress(request != null ? request.getIpAddress() : null)
                .userAgent(request != null ? request.getUserAgent() : null)
                .build();
        qrScanLogRepository.save(log);
    }

    private QRScanLog.QRScanLogDTO toScanDTO(QRScanLog log) {
        QRScanLog.QRScanLogDTO dto = new QRScanLog.QRScanLogDTO();
        dto.setId(log.getId());
        dto.setQrCode(log.getQrCode());
        dto.setEmployeeId(log.getEmployeeId());
        dto.setScanType(log.getScanType());
        dto.setShiftType(log.getShiftType());
        dto.setScanTime(log.getScanTime());
        dto.setStatus(log.getStatus());
        dto.setErrorMessage(log.getErrorMessage());
        return dto;
    }
}
