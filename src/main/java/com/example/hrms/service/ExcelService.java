package com.example.hrms.service;

import com.example.hrms.dto.AttendanceDTO;
import com.example.hrms.dto.EmployeeDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ExcelService {
    byte[] exportAttendanceToExcel(List<AttendanceDTO> attendances);
    List<AttendanceDTO> importAttendanceFromExcel(MultipartFile file) throws Exception;
    
    // Employee methods
    byte[] exportEmployeesToExcel(List<EmployeeDTO> employees);
    List<EmployeeDTO> importEmployeesFromExcel(MultipartFile file) throws Exception;
}
