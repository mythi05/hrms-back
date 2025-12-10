package com.example.hrms.service.impl;

import com.example.hrms.dto.AttendanceDTO;
import com.example.hrms.dto.EmployeeDTO;
import com.example.hrms.entity.Attendance;
import com.example.hrms.service.ExcelService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExcelServiceImpl implements ExcelService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public byte[] exportAttendanceToExcel(List<AttendanceDTO> attendances) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Chấm Công");

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // Data style
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            // Create headers
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "Mã NV", "Họ tên", "Ngày", "Giờ vào", "Giờ ra", "Tổng giờ", "Trạng thái", "Ghi chú"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Create data rows
            int rowNum = 1;
            for (AttendanceDTO attendance : attendances) {
                Row row = sheet.createRow(rowNum++);
                
                row.createCell(0).setCellValue(attendance.getId() != null ? attendance.getId() : 0);
                row.createCell(1).setCellValue(attendance.getEmployeeId() != null ? attendance.getEmployeeId() : 0);
                row.createCell(2).setCellValue(attendance.getEmployeeName() != null ? attendance.getEmployeeName() : "");
                
                // Format date
                Cell dateCell = row.createCell(3);
                if (attendance.getDate() != null) {
                    dateCell.setCellValue(attendance.getDate().format(DATE_FORMATTER));
                } else {
                    dateCell.setCellValue("");
                }
                
                // Format times
                Cell checkInCell = row.createCell(4);
                if (attendance.getCheckIn() != null) {
                    checkInCell.setCellValue(attendance.getCheckIn().format(TIME_FORMATTER));
                } else {
                    checkInCell.setCellValue("");
                }
                
                Cell checkOutCell = row.createCell(5);
                if (attendance.getCheckOut() != null) {
                    checkOutCell.setCellValue(attendance.getCheckOut().format(TIME_FORMATTER));
                } else {
                    checkOutCell.setCellValue("");
                }
                
                row.createCell(6).setCellValue(attendance.getTotalHours() != null ? attendance.getTotalHours().doubleValue() : 0);
                row.createCell(7).setCellValue(attendance.getStatus() != null ? attendance.getStatus() : "PRESENT");
                row.createCell(8).setCellValue(attendance.getNote() != null ? attendance.getNote() : "");

                // Apply data style
                for (int i = 0; i < headers.length; i++) {
                    row.getCell(i).setCellStyle(dataStyle);
                }
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi xuất file Excel", e);
        }
    }

    @Override
    public List<AttendanceDTO> importAttendanceFromExcel(MultipartFile file) throws Exception {
        List<AttendanceDTO> attendances = new ArrayList<>();
        String filename = file.getOriginalFilename().toLowerCase();
        
        // Check file extension
        if (!filename.endsWith(".xlsx") && !filename.endsWith(".xls") && !filename.endsWith(".csv")) {
            throw new Exception("Chỉ chấp nhận file Excel (.xlsx, .xls) hoặc CSV (.csv)");
        }

        // Handle CSV file
        if (filename.endsWith(".csv")) {
            return importAttendanceFromCSV(file);
        }

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            
            // Skip header row
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    AttendanceDTO attendance = new AttendanceDTO();
                    
                    // ID (column 0) - optional for import
                    Cell idCell = row.getCell(0);
                    if (idCell != null && idCell.getCellType() != CellType.BLANK) {
                        attendance.setId((long) idCell.getNumericCellValue());
                    }
                    
                    // Employee ID (column 1) - required
                    Cell empIdCell = row.getCell(1);
                    if (empIdCell == null || empIdCell.getCellType() == CellType.BLANK) {
                        throw new Exception("Dòng " + (i + 1) + ": Thiếu mã nhân viên");
                    }
                    attendance.setEmployeeId((long) empIdCell.getNumericCellValue());
                    
                    // Employee Name (column 2) - optional
                    Cell nameCell = row.getCell(2);
                    if (nameCell != null && nameCell.getCellType() != CellType.BLANK) {
                        attendance.setEmployeeName(nameCell.getStringCellValue());
                    }
                    
                    // Date (column 3) - required
                    Cell dateCell = row.getCell(3);
                    if (dateCell == null || dateCell.getCellType() == CellType.BLANK) {
                        throw new Exception("Dòng " + (i + 1) + ": Thiếu ngày");
                    }
                    
                    LocalDate date;
                    if (dateCell.getCellType() == CellType.STRING) {
                        date = LocalDate.parse(dateCell.getStringCellValue(), DATE_FORMATTER);
                    } else {
                        date = dateCell.getLocalDateTimeCellValue().toLocalDate();
                    }
                    attendance.setDate(date);
                    
                    // Check In (column 4) - optional
                    Cell checkInCell = row.getCell(4);
                    if (checkInCell != null && checkInCell.getCellType() != CellType.BLANK) {
                        if (checkInCell.getCellType() == CellType.STRING) {
                            attendance.setCheckIn(LocalTime.parse(checkInCell.getStringCellValue(), TIME_FORMATTER));
                        } else {
                            attendance.setCheckIn(checkInCell.getLocalDateTimeCellValue().toLocalTime());
                        }
                    }
                    
                    // Check Out (column 5) - optional
                    Cell checkOutCell = row.getCell(5);
                    if (checkOutCell != null && checkOutCell.getCellType() != CellType.BLANK) {
                        if (checkOutCell.getCellType() == CellType.STRING) {
                            attendance.setCheckOut(LocalTime.parse(checkOutCell.getStringCellValue(), TIME_FORMATTER));
                        } else {
                            attendance.setCheckOut(checkOutCell.getLocalDateTimeCellValue().toLocalTime());
                        }
                    }
                    
                    // Total Hours (column 6) - optional
                    Cell hoursCell = row.getCell(6);
                    if (hoursCell != null && hoursCell.getCellType() != CellType.BLANK) {
                        attendance.setTotalHours(java.math.BigDecimal.valueOf(hoursCell.getNumericCellValue()));
                    }
                    
                    // Status (column 7) - optional
                    Cell statusCell = row.getCell(7);
                    if (statusCell != null && statusCell.getCellType() != CellType.BLANK) {
                        attendance.setStatus(statusCell.getStringCellValue().trim());
                    } else {
                        attendance.setStatus("PRESENT");
                    }
                    
                    // Note (column 8) - optional
                    Cell noteCell = row.getCell(8);
                    if (noteCell != null && noteCell.getCellType() != CellType.BLANK) {
                        attendance.setNote(noteCell.getStringCellValue());
                    }

                    attendances.add(attendance);

                } catch (Exception e) {
                    throw new Exception("Lỗi ở dòng " + (i + 1) + ": " + e.getMessage());
                }
            }
        }

        return attendances;
    }

    private List<AttendanceDTO> importAttendanceFromCSV(MultipartFile file) throws Exception {
        List<AttendanceDTO> attendances = new ArrayList<>();
        
        try (InputStream inputStream = file.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            
            String line;
            int lineNumber = 0;
            
            // Skip header
            reader.readLine();
            lineNumber++;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                
                try {
                    String[] values = line.split(",");
                    if (values.length < 8) {
                        throw new Exception("Không đủ cột dữ liệu");
                    }
                    
                    AttendanceDTO attendance = new AttendanceDTO();
                    
                    // ID (column 0) - optional
                    if (!values[0].trim().isEmpty()) {
                        attendance.setId(Long.parseLong(values[0].trim()));
                    }
                    
                    // Employee ID (column 1) - required
                    if (values[1].trim().isEmpty()) {
                        throw new Exception("Thiếu mã nhân viên");
                    }
                    attendance.setEmployeeId(Long.parseLong(values[1].trim()));
                    
                    // Employee Name (column 2) - optional
                    if (!values[2].trim().isEmpty()) {
                        attendance.setEmployeeName(values[2].trim());
                    }
                    
                    // Date (column 3) - required
                    if (values[3].trim().isEmpty()) {
                        throw new Exception("Thiếu ngày");
                    }
                    attendance.setDate(LocalDate.parse(values[3].trim(), DATE_FORMATTER));
                    
                    // Check In (column 4) - optional
                    if (!values[4].trim().isEmpty()) {
                        attendance.setCheckIn(LocalTime.parse(values[4].trim(), TIME_FORMATTER));
                    }
                    
                    // Check Out (column 5) - optional
                    if (!values[5].trim().isEmpty()) {
                        attendance.setCheckOut(LocalTime.parse(values[5].trim(), TIME_FORMATTER));
                    }
                    
                    // Total Hours (column 6) - optional
                    if (!values[6].trim().isEmpty()) {
                        attendance.setTotalHours(java.math.BigDecimal.valueOf(Double.parseDouble(values[6].trim())));
                    }
                    
                    // Status (column 7) - optional
                    if (!values[7].trim().isEmpty()) {
                        attendance.setStatus(values[7].trim());
                    } else {
                        attendance.setStatus("PRESENT");
                    }
                    
                    // Note (column 8) - optional
                    if (values.length > 8 && !values[8].trim().isEmpty()) {
                        attendance.setNote(values[8].trim());
                    }
                    
                    attendances.add(attendance);
                    
                } catch (Exception e) {
                    throw new Exception("Lỗi ở dòng " + lineNumber + ": " + e.getMessage());
                }
            }
        }
        
        return attendances;
    }

    // Employee methods - Simple implementation
    @Override
    public byte[] exportEmployeesToExcel(List<EmployeeDTO> employees) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Nhân Viên");

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Create headers
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "Mã NV", "Họ tên", "Email", "SĐT", "Phòng ban", "Chức vụ"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Create data rows
            int rowNum = 1;
            for (EmployeeDTO employee : employees) {
                Row row = sheet.createRow(rowNum++);
                
                row.createCell(0).setCellValue(employee.getId() != null ? employee.getId() : 0);
                row.createCell(1).setCellValue(employee.getEmployeeCode() != null ? employee.getEmployeeCode() : "");
                row.createCell(2).setCellValue(employee.getFullName() != null ? employee.getFullName() : "");
                row.createCell(3).setCellValue(employee.getEmail() != null ? employee.getEmail() : "");
                row.createCell(4).setCellValue(employee.getPhone() != null ? employee.getPhone() : "");
                row.createCell(5).setCellValue(employee.getDepartment() != null ? employee.getDepartment() : "");
                row.createCell(6).setCellValue(employee.getPosition() != null ? employee.getPosition() : "");
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi xuất file Excel", e);
        }
    }

    @Override
    public List<EmployeeDTO> importEmployeesFromExcel(MultipartFile file) throws Exception {
        List<EmployeeDTO> employees = new ArrayList<>();
        String filename = file.getOriginalFilename().toLowerCase();
        
        // Check file extension
        if (!filename.endsWith(".xlsx") && !filename.endsWith(".xls") && !filename.endsWith(".csv")) {
            throw new Exception("Chỉ chấp nhận file Excel (.xlsx, .xls) hoặc CSV (.csv)");
        }

        // Handle CSV file
        if (filename.endsWith(".csv")) {
            return importEmployeesFromCSV(file);
        }

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            
            // Skip header row
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    EmployeeDTO employee = new EmployeeDTO();
                    
                    // Employee Code (column 1) - required
                    Cell empCodeCell = row.getCell(1);
                    if (empCodeCell == null || empCodeCell.getCellType() == CellType.BLANK) {
                        throw new Exception("Dòng " + (i + 1) + ": Thiếu mã nhân viên");
                    }
                    employee.setEmployeeCode(empCodeCell.getStringCellValue());
                    
                    // Full Name (column 2) - required
                    Cell nameCell = row.getCell(2);
                    if (nameCell == null || nameCell.getCellType() == CellType.BLANK) {
                        throw new Exception("Dòng " + (i + 1) + ": Thiếu họ tên");
                    }
                    employee.setFullName(nameCell.getStringCellValue());
                    
                    // Email (column 3) - optional
                    Cell emailCell = row.getCell(3);
                    if (emailCell != null && emailCell.getCellType() != CellType.BLANK) {
                        employee.setEmail(emailCell.getStringCellValue());
                    }
                    
                    // Phone (column 4) - optional
                    Cell phoneCell = row.getCell(4);
                    if (phoneCell != null && phoneCell.getCellType() != CellType.BLANK) {
                        employee.setPhone(phoneCell.getStringCellValue());
                    }
                    
                    // Department (column 5) - optional
                    Cell deptCell = row.getCell(5);
                    if (deptCell != null && deptCell.getCellType() != CellType.BLANK) {
                        employee.setDepartment(deptCell.getStringCellValue());
                    }
                    
                    // Position (column 6) - optional
                    Cell posCell = row.getCell(6);
                    if (posCell != null && posCell.getCellType() != CellType.BLANK) {
                        employee.setPosition(posCell.getStringCellValue());
                    }

                    employees.add(employee);

                } catch (Exception e) {
                    throw new Exception("Lỗi ở dòng " + (i + 1) + ": " + e.getMessage());
                }
            }
        }

        return employees;
    }

    private List<EmployeeDTO> importEmployeesFromCSV(MultipartFile file) throws Exception {
        List<EmployeeDTO> employees = new ArrayList<>();
        
        try (InputStream inputStream = file.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            
            String line;
            int lineNumber = 0;
            
            // Skip header
            reader.readLine();
            lineNumber++;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                
                try {
                    String[] values = line.split(",");
                    if (values.length < 3) {
                        throw new Exception("Không đủ cột dữ liệu");
                    }
                    
                    EmployeeDTO employee = new EmployeeDTO();
                    
                    // Employee Code (column 1) - required
                    if (values[1].trim().isEmpty()) {
                        throw new Exception("Thiếu mã nhân viên");
                    }
                    employee.setEmployeeCode(values[1].trim());
                    
                    // Full Name (column 2) - required
                    if (values[2].trim().isEmpty()) {
                        throw new Exception("Thiếu họ tên");
                    }
                    employee.setFullName(values[2].trim());
                    
                    // Email (column 3) - optional
                    if (values.length > 3 && !values[3].trim().isEmpty()) {
                        employee.setEmail(values[3].trim());
                    }
                    
                    // Phone (column 4) - optional
                    if (values.length > 4 && !values[4].trim().isEmpty()) {
                        employee.setPhone(values[4].trim());
                    }
                    
                    // Department (column 5) - optional
                    if (values.length > 5 && !values[5].trim().isEmpty()) {
                        employee.setDepartment(values[5].trim());
                    }
                    
                    // Position (column 6) - optional
                    if (values.length > 6 && !values[6].trim().isEmpty()) {
                        employee.setPosition(values[6].trim());
                    }
                    
                    employees.add(employee);
                    
                } catch (Exception e) {
                    throw new Exception("Lỗi ở dòng " + lineNumber + ": " + e.getMessage());
                }
            }
        }
        
        return employees;
    }

    private String getStatusText(Attendance.Status status) {
        if (status == null) return "";
        switch (status) {
            case PRESENT: return "Có mặt";
            case LATE: return "Đi muộn";
            case ABSENT: return "Vắng mặt";
            case LEAVE: return "Nghỉ phép";
            case HOLIDAY: return "Ngày lễ";
            default: return status.toString();
        }
    }

    private Attendance.Status getStatusFromText(String text) {
        if (text == null || text.trim().isEmpty()) return Attendance.Status.PRESENT;
        
        switch (text.trim().toLowerCase()) {
            case "có mặt":
            case "present":
                return Attendance.Status.PRESENT;
            case "đi muộn":
            case "late":
                return Attendance.Status.LATE;
            case "vắng mặt":
            case "absent":
                return Attendance.Status.ABSENT;
            case "nghỉ phép":
            case "leave":
                return Attendance.Status.LEAVE;
            case "ngày lễ":
            case "holiday":
                return Attendance.Status.HOLIDAY;
            default:
                return Attendance.Status.PRESENT;
        }
    }
}
