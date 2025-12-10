package com.example.hrms.service;

import com.example.hrms.dto.PayrollDTO;

import java.math.BigDecimal;
import java.util.List;

public interface PayrollService {

    PayrollDTO createOrUpdate(PayrollDTO dto);

    PayrollDTO getPayrollForEmployeeMonth(Long employeeId, Integer month, Integer year);

    List<PayrollDTO> getPayrollHistoryForEmployee(Long employeeId, int months);

    List<PayrollDTO> getPayrollForMonth(Integer month, Integer year);

    List<PayrollDTO> getAllPayroll();

    PayrollDTO markPaid(Long id);

    PayrollDTO markPending(Long id);

    PayrollDTO calculatePayroll(Long employeeId, Integer month, Integer year, BigDecimal basicSalary);
}
