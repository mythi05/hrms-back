package com.example.hrms.service.impl;

import com.example.hrms.dto.PayrollDTO;
import com.example.hrms.entity.Attendance;
import com.example.hrms.entity.Employee;
import com.example.hrms.entity.Payroll;
import com.example.hrms.entity.Notification.NotificationType;
import com.example.hrms.service.NotificationService;
import com.example.hrms.exception.ResourceNotFoundException;
import com.example.hrms.mapper.PayrollMapper;
import com.example.hrms.repository.AttendanceRepository;
import com.example.hrms.repository.EmployeeRepository;
import com.example.hrms.repository.PayrollRepository;
import com.example.hrms.service.PayrollService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PayrollServiceImpl implements PayrollService {

    private final PayrollRepository payrollRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final NotificationService notificationService;

    @Override
    public PayrollDTO createOrUpdate(PayrollDTO dto) {
        Payroll payroll = payrollRepository
                .findByEmployeeIdAndMonthAndYear(dto.getEmployeeId(), dto.getMonth(), dto.getYear())
                .orElseGet(Payroll::new);

        Payroll.PaymentStatus oldStatus = payroll.getPaymentStatus();

        payroll.setEmployeeId(dto.getEmployeeId());
        payroll.setMonth(dto.getMonth());
        payroll.setYear(dto.getYear());
        payroll.setBasicSalary(nvl(dto.getBasicSalary()));
        payroll.setAllowances(nvl(dto.getAllowances()));
        payroll.setBonus(nvl(dto.getBonus()));
        payroll.setOvertimePay(nvl(dto.getOvertimePay()));

        // Tính lương gộp
        BigDecimal gross = payroll.getBasicSalary()
                .add(payroll.getAllowances())
                .add(payroll.getBonus())
                .add(payroll.getOvertimePay());
        payroll.setGrossSalary(gross);

        payroll.setSocialInsurance(nvl(dto.getSocialInsurance()));
        payroll.setHealthInsurance(nvl(dto.getHealthInsurance()));
        payroll.setUnemploymentInsurance(nvl(dto.getUnemploymentInsurance()));
        payroll.setPersonalIncomeTax(nvl(dto.getPersonalIncomeTax()));
        payroll.setOtherDeductions(nvl(dto.getOtherDeductions()));

        BigDecimal totalDeduction = payroll.getSocialInsurance()
                .add(payroll.getHealthInsurance())
                .add(payroll.getUnemploymentInsurance())
                .add(payroll.getPersonalIncomeTax())
                .add(payroll.getOtherDeductions());
        payroll.setTotalDeductions(totalDeduction);

        BigDecimal net = gross.subtract(totalDeduction);
        payroll.setNetSalary(net);

        if (dto.getPaymentStatus() != null) {
            payroll.setPaymentStatus(Payroll.PaymentStatus.valueOf(dto.getPaymentStatus()));
        }
        payroll.setPaymentDate(dto.getPaymentDate());

        Payroll saved = payrollRepository.save(payroll);
        Employee emp = employeeRepository.findById(saved.getEmployeeId()).orElse(null);

        if (emp != null) {
            // Thông báo cập nhật lương mỗi lần chỉnh sửa bảng lương
            String updateTitle = "Bảng lương của bạn đã được cập nhật";
            String updateMessage = "Lương tháng " + saved.getMonth() + "/" + saved.getYear()
                    + " đã được điều chỉnh. Lương thực nhận hiện tại: " + saved.getNetSalary() + ".";
            notificationService.createNotification(emp.getId(), updateTitle, updateMessage, NotificationType.PAYROLL_UPDATED);

            // Thông báo thanh toán lương khi chuyển trạng thái sang PAID lần đầu
            if (saved.getPaymentStatus() == Payroll.PaymentStatus.PAID
                    && oldStatus != Payroll.PaymentStatus.PAID) {
                String paidTitle = "Lương tháng " + saved.getMonth() + "/" + saved.getYear() + " đã được thanh toán";
                String paidMessage = "Lương thực nhận: " + saved.getNetSalary() + ". Vui lòng kiểm tra chi tiết trong mục Lương.";
                notificationService.createNotification(emp.getId(), paidTitle, paidMessage, NotificationType.PAYROLL);
            }
        }

        return PayrollMapper.toDTO(saved, emp);
    }

    @Override
    public PayrollDTO getPayrollForEmployeeMonth(Long employeeId, Integer month, Integer year) {
        YearMonth ym = YearMonth.now();
        int m = (month != null) ? month : ym.getMonthValue();
        int y = (year != null) ? year : ym.getYear();
        Payroll payroll = payrollRepository
                .findByEmployeeIdAndMonthAndYear(employeeId, m, y)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll not found"));
        Employee emp = employeeRepository.findById(employeeId).orElse(null);
        return PayrollMapper.toDTO(payroll, emp);
    }

    @Override
    public List<PayrollDTO> getPayrollHistoryForEmployee(Long employeeId, int months) {
        List<Payroll> list = payrollRepository.findByEmployeeId(employeeId);
        YearMonth now = YearMonth.now();
        YearMonth from = now.minusMonths(months - 1L);

        return list.stream()
                .filter(p -> {
                    YearMonth ym = YearMonth.of(p.getYear(), p.getMonth());
                    return (ym.equals(from) || ym.isAfter(from)) && (ym.equals(now) || ym.isBefore(now) || ym.equals(now));
                })
                .sorted(Comparator.comparing(Payroll::getYear).thenComparing(Payroll::getMonth).reversed())
                .map(p -> PayrollMapper.toDTO(p, employeeRepository.findById(p.getEmployeeId()).orElse(null)))
                .collect(Collectors.toList());
    }

    @Override
    public List<PayrollDTO> getPayrollForMonth(Integer month, Integer year) {
        YearMonth ym = YearMonth.now();
        int m = (month != null) ? month : ym.getMonthValue();
        int y = (year != null) ? year : ym.getYear();
        List<Payroll> list = payrollRepository.findByMonthAndYear(m, y);
        return list.stream()
                .map(p -> PayrollMapper.toDTO(p, employeeRepository.findById(p.getEmployeeId()).orElse(null)))
                .collect(Collectors.toList());
    }

    @Override
    public List<PayrollDTO> getAllPayroll() {
        return payrollRepository.findAll().stream()
                .map(p -> PayrollMapper.toDTO(p, employeeRepository.findById(p.getEmployeeId()).orElse(null)))
                .collect(Collectors.toList());
    }

    @Override
    public PayrollDTO markPaid(Long id) {
        Payroll payroll = payrollRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll not found"));
        payroll.setPaymentStatus(Payroll.PaymentStatus.PAID);
        payroll.setPaymentDate(LocalDate.now());
        Payroll saved = payrollRepository.save(payroll);
        Employee emp = employeeRepository.findById(saved.getEmployeeId()).orElse(null);

        if (emp != null) {
            String title = "Lương tháng " + saved.getMonth() + "/" + saved.getYear() + " đã được thanh toán";
            String message = "Lương thực nhận: " + saved.getNetSalary() + ". Vui lòng kiểm tra chi tiết trong mục Lương.";
            notificationService.createNotification(emp.getId(), title, message, NotificationType.PAYROLL);
        }
        return PayrollMapper.toDTO(saved, emp);
    }

    @Override
    public PayrollDTO markPending(Long id) {
        Payroll payroll = payrollRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll not found"));
        payroll.setPaymentStatus(Payroll.PaymentStatus.PENDING);
        Payroll saved = payrollRepository.save(payroll);
        Employee emp = employeeRepository.findById(saved.getEmployeeId()).orElse(null);
        return PayrollMapper.toDTO(saved, emp);
    }

    @Override
    public PayrollDTO calculatePayroll(Long employeeId, Integer month, Integer year, BigDecimal basicSalary) {
        // Lấy dữ liệu chấm công của nhân viên trong tháng
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        
        List<Attendance> attendances = attendanceRepository.findByEmployeeIdAndDateBetween(employeeId, startDate, endDate);
        
        // Đếm số ngày đi làm và các loại ngày khác
        long presentDays = attendances.stream()
                .filter(a -> a.getStatus() == Attendance.Status.PRESENT || a.getStatus() == Attendance.Status.LATE)
                .count();
        long lateDays = attendances.stream()
                .filter(a -> a.getStatus() == Attendance.Status.LATE)
                .count();
        long absentDays = attendances.stream()
                .filter(a -> a.getStatus() == Attendance.Status.ABSENT)
                .count();
        long leaveDays = attendances.stream()
                .filter(a -> a.getStatus() == Attendance.Status.LEAVE)
                .count();
        
        // Tính lương theo số ngày đi làm thực tế
        // Giả định 22 ngày làm việc chuẩn/tháng (trừ chủ nhật)
        BigDecimal dailySalary = basicSalary.divide(BigDecimal.valueOf(22), 2, BigDecimal.ROUND_HALF_UP);
        BigDecimal actualSalary = dailySalary.multiply(BigDecimal.valueOf(presentDays));
        
        // Phạt đi muộn: trừ 50% lương ngày cho mỗi ngày đi muộn
        BigDecimal lateDeduction = dailySalary.multiply(BigDecimal.valueOf(lateDays)).multiply(BigDecimal.valueOf(0.5));
        
        // Phạt nghỉ không phép: trừ 100% lương ngày cho mỗi ngày vắng mặt
        BigDecimal absentDeduction = dailySalary.multiply(BigDecimal.valueOf(absentDays));
        
        // Lương cơ bản sau khi áp dụng chấm công
        BigDecimal adjustedBasicSalary = actualSalary.subtract(lateDeduction).subtract(absentDeduction);
        
        // Tính bảo hiểm (dựa trên lương cơ bản gốc, không phải lương đã điều chỉnh)
        // BHXH: 8%, BHYT: 1.5%, BHTN: 1% -> Tổng: 10.5%
        BigDecimal socialInsurance = basicSalary.multiply(BigDecimal.valueOf(0.08)); // 8% BHXH
        BigDecimal healthInsurance = basicSalary.multiply(BigDecimal.valueOf(0.015)); // 1.5% BHYT
        BigDecimal unemploymentInsurance = basicSalary.multiply(BigDecimal.valueOf(0.01)); // 1% BHTN
        
        // Loại bỏ thuế cá nhân - chỉ tính bảo hiểm
        BigDecimal personalIncomeTax = BigDecimal.ZERO; // Không tính thuế TNCN
        
        // Tổng các khoản khấu trừ khác
        BigDecimal otherDeductions = lateDeduction.add(absentDeduction);
        
        // Tạo DTO kết quả
        PayrollDTO dto = new PayrollDTO();
        dto.setEmployeeId(employeeId);
        dto.setMonth(month);
        dto.setYear(year);
        dto.setBasicSalary(adjustedBasicSalary);
        dto.setAllowances(BigDecimal.ZERO);
        dto.setBonus(BigDecimal.ZERO);
        dto.setOvertimePay(BigDecimal.ZERO);
        dto.setGrossSalary(adjustedBasicSalary);
        dto.setSocialInsurance(socialInsurance);
        dto.setHealthInsurance(healthInsurance);
        dto.setUnemploymentInsurance(unemploymentInsurance);
        dto.setPersonalIncomeTax(personalIncomeTax); // = 0
        dto.setOtherDeductions(otherDeductions);
        
        // Tổng khấu trừ = Bảo hiểm (10.5%) + Khấu trừ khác
        BigDecimal totalDeductions = socialInsurance.add(healthInsurance).add(unemploymentInsurance).add(otherDeductions);
        dto.setTotalDeductions(totalDeductions);
        
        // Lương thực nhận = Lương đã điều chỉnh - Tổng khấu trừ
        dto.setNetSalary(adjustedBasicSalary.subtract(totalDeductions));
        dto.setPaymentStatus("PENDING");
        
        // Lấy thông tin nhân viên
        Employee emp = employeeRepository.findById(employeeId).orElse(null);
        if (emp != null) {
            dto.setEmployeeName(emp.getFullName());
        }
        
        return dto;
    }

    private BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
