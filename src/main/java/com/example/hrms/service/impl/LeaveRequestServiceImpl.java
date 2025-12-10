package com.example.hrms.service.impl;

import com.example.hrms.dto.LeaveRequestDTO;
import com.example.hrms.entity.Employee;
import com.example.hrms.entity.LeaveRequest;
import com.example.hrms.entity.Notification.NotificationType;
import com.example.hrms.service.NotificationService;
import com.example.hrms.exception.ResourceNotFoundException;
import com.example.hrms.mapper.LeaveRequestMapper;
import com.example.hrms.repository.EmployeeRepository;
import com.example.hrms.repository.LeaveRequestRepository;
import com.example.hrms.service.LeaveRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaveRequestServiceImpl implements LeaveRequestService {

    private final LeaveRequestRepository leaveRepo;
    private final EmployeeRepository employeeRepo;
    private final NotificationService notificationService;

    // Quota mặc định cho từng loại nghỉ (có thể sau này cấu hình từ DB)
    private static final BigDecimal ANNUAL_QUOTA   = BigDecimal.valueOf(12);
    private static final BigDecimal SICK_QUOTA     = BigDecimal.valueOf(10);
    private static final BigDecimal MARRIAGE_QUOTA = BigDecimal.valueOf(3);
    private static final BigDecimal OTHER_QUOTA    = BigDecimal.valueOf(5);

    @Override
    public LeaveRequestDTO createRequest(LeaveRequestDTO dto) {
        // Tính số ngày nghỉ nếu chưa truyền từ frontend
        if (dto.getDaysCount() == null && dto.getStartDate() != null && dto.getEndDate() != null) {
            long days = ChronoUnit.DAYS.between(dto.getStartDate(), dto.getEndDate()) + 1;
            dto.setDaysCount(BigDecimal.valueOf(days));
        }

        LeaveRequest entity = LeaveRequestMapper.toEntity(dto);
        entity.setStatus(LeaveRequest.Status.PENDING);
        entity.setApprovedAt(null);
        entity.setRejectReason(null);

        LeaveRequest saved = leaveRepo.save(entity);
        LeaveRequestDTO dtoSaved = LeaveRequestMapper.toDTO(saved);
        fillApproverName(dtoSaved);
        return dtoSaved;
    }

    @Override
    public List<LeaveRequestDTO> getMyRequests(Long employeeId) {
        return leaveRepo.findByEmployeeId(employeeId)
                .stream()
                .map(LeaveRequestMapper::toDTO)
                .peek(this::fillApproverName)
                .collect(Collectors.toList());
    }

    @Override
    public List<LeaveRequestDTO> getAllRequests() {
        return leaveRepo.findAll()
                .stream()
                .map(LeaveRequestMapper::toDTO)
                .peek(this::fillApproverName)
                .collect(Collectors.toList());
    }

    @Override
    public LeaveRequestDTO approveRequest(Long id, Long approverId, String note) {
        LeaveRequest lr = leaveRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));

        // Kiểm tra quota trước khi duyệt
        LocalDate now = LocalDate.now();
        int year = now.getYear();

        BigDecimal quota;
        switch (lr.getLeaveType()) {
            case ANNUAL -> quota = ANNUAL_QUOTA;
            case SICK -> quota = SICK_QUOTA;
            case MARRIAGE -> quota = MARRIAGE_QUOTA;
            case MATERNITY, UNPAID, OTHER -> quota = OTHER_QUOTA;
            default -> quota = OTHER_QUOTA;
        }

        // Tính số ngày đã duyệt cùng loại trong năm hiện tại
        BigDecimal used = leaveRepo.findByEmployeeIdAndStatus(lr.getEmployeeId(), LeaveRequest.Status.APPROVED)
                .stream()
                .filter(r -> {
                    LocalDate s = r.getStartDate();
                    LocalDate e = r.getEndDate();
                    return (s != null && s.getYear() == year) || (e != null && e.getYear() == year);
                })
                .filter(r -> r.getLeaveType() == lr.getLeaveType())
                .map(LeaveRequest::getDaysCount)
                .filter(d -> d != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal current = lr.getDaysCount() != null ? lr.getDaysCount() : BigDecimal.ZERO;
        BigDecimal totalAfterApprove = used.add(current);

        if (totalAfterApprove.compareTo(quota) > 0) {
            throw new RuntimeException("Số ngày nghỉ cho loại " + lr.getLeaveType() + " đã vượt quá quota cho phép trong năm");
        }

        lr.setStatus(LeaveRequest.Status.APPROVED);
        lr.setApproverId(approverId);
        lr.setApprovedAt(LocalDateTime.now());
        if (note != null && !note.isBlank()) {
            lr.setRejectReason(null);
        }

        LeaveRequest saved = leaveRepo.save(lr);
        LeaveRequestDTO dto = LeaveRequestMapper.toDTO(saved);
        fillApproverName(dto);

        // Gửi thông báo cho nhân viên: đơn nghỉ đã được duyệt
        Employee emp = employeeRepo.findById(saved.getEmployeeId()).orElse(null);
        if (emp != null) {
            String title = "Đơn nghỉ phép đã được duyệt";
            String message = "Đơn nghỉ " + saved.getLeaveType() + " từ " + saved.getStartDate() + " đến " + saved.getEndDate() + " đã được duyệt.";
            notificationService.createNotification(emp.getId(), title, message, NotificationType.LEAVE_APPROVED);
        }
        return dto;
    }

    @Override
    public LeaveRequestDTO rejectRequest(Long id, Long approverId, String rejectReason) {
        LeaveRequest lr = leaveRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));

        lr.setStatus(LeaveRequest.Status.REJECTED);
        lr.setApproverId(approverId);
        lr.setApprovedAt(LocalDateTime.now());
        lr.setRejectReason(rejectReason);

        LeaveRequest saved = leaveRepo.save(lr);
        LeaveRequestDTO dto = LeaveRequestMapper.toDTO(saved);
        fillApproverName(dto);

        // Gửi thông báo cho nhân viên: đơn nghỉ bị từ chối
        Employee emp = employeeRepo.findById(saved.getEmployeeId()).orElse(null);
        if (emp != null) {
            String title = "Đơn nghỉ phép bị từ chối";
            String reason = (rejectReason != null && !rejectReason.isBlank()) ? (" Lý do: " + rejectReason) : "";
            String message = "Đơn nghỉ " + saved.getLeaveType() + " từ " + saved.getStartDate() + " đến " + saved.getEndDate() + " đã bị từ chối." + reason;
            notificationService.createNotification(emp.getId(), title, message, NotificationType.LEAVE_REJECTED);
        }
        return dto;
    }

    /**
     * Gán approverName cho DTO nếu có approverId.
     */
    private void fillApproverName(LeaveRequestDTO dto) {
        if (dto.getApproverId() == null) return;
        employeeRepo.findById(dto.getApproverId())
                .map(Employee::getFullName)
                .ifPresent(dto::setApproverName);
    }
}
