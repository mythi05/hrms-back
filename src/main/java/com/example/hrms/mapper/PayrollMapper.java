package com.example.hrms.mapper;

import com.example.hrms.dto.PayrollDTO;
import com.example.hrms.entity.Employee;
import com.example.hrms.entity.Payroll;

public class PayrollMapper {

    public static PayrollDTO toDTO(Payroll payroll, Employee employee) {
        if (payroll == null) return null;
        PayrollDTO dto = new PayrollDTO();
        dto.setId(payroll.getId());
        dto.setEmployeeId(payroll.getEmployeeId());
        if (employee != null) {
            dto.setEmployeeName(employee.getFullName());
        }
        dto.setMonth(payroll.getMonth());
        dto.setYear(payroll.getYear());
        dto.setBasicSalary(payroll.getBasicSalary());
        dto.setAllowances(payroll.getAllowances());
        dto.setBonus(payroll.getBonus());
        dto.setOvertimePay(payroll.getOvertimePay());
        dto.setGrossSalary(payroll.getGrossSalary());
        dto.setSocialInsurance(payroll.getSocialInsurance());
        dto.setHealthInsurance(payroll.getHealthInsurance());
        dto.setUnemploymentInsurance(payroll.getUnemploymentInsurance());
        dto.setPersonalIncomeTax(payroll.getPersonalIncomeTax());
        dto.setOtherDeductions(payroll.getOtherDeductions());
        dto.setTotalDeductions(payroll.getTotalDeductions());
        dto.setNetSalary(payroll.getNetSalary());
        dto.setPaymentStatus(payroll.getPaymentStatus() != null ? payroll.getPaymentStatus().name() : null);
        dto.setPaymentDate(payroll.getPaymentDate());
        return dto;
    }

    public static Payroll toEntity(PayrollDTO dto) {
        if (dto == null) return null;
        Payroll payroll = Payroll.builder()
                .id(dto.getId())
                .employeeId(dto.getEmployeeId())
                .month(dto.getMonth())
                .year(dto.getYear())
                .basicSalary(dto.getBasicSalary())
                .allowances(dto.getAllowances())
                .bonus(dto.getBonus())
                .overtimePay(dto.getOvertimePay())
                .grossSalary(dto.getGrossSalary())
                .socialInsurance(dto.getSocialInsurance())
                .healthInsurance(dto.getHealthInsurance())
                .unemploymentInsurance(dto.getUnemploymentInsurance())
                .personalIncomeTax(dto.getPersonalIncomeTax())
                .otherDeductions(dto.getOtherDeductions())
                .totalDeductions(dto.getTotalDeductions())
                .netSalary(dto.getNetSalary())
                .build();
        return payroll;
    }
}
