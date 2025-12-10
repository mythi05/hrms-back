package com.example.hrms.controller;

import com.example.hrms.dto.AttendanceDTO;
import com.example.hrms.service.AttendanceService;
import com.example.hrms.service.ExcelService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
@CrossOrigin("*")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final ExcelService excelService;

    @PostMapping("/check-in/{employeeId}")
    public ResponseEntity<AttendanceDTO> checkIn(@PathVariable Long employeeId) {
        AttendanceDTO dto = attendanceService.checkIn(employeeId);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/check-out/{employeeId}")
    public ResponseEntity<AttendanceDTO> checkOut(@PathVariable Long employeeId) {
        AttendanceDTO dto = attendanceService.checkOut(employeeId);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/today/{employeeId}")
    public ResponseEntity<AttendanceDTO> getToday(@PathVariable Long employeeId) {
        AttendanceDTO dto = attendanceService.getTodayAttendance(employeeId);
        if (dto == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/history/{employeeId}")
    public ResponseEntity<List<AttendanceDTO>> getHistory(@PathVariable Long employeeId) {
        List<AttendanceDTO> list = attendanceService.getAttendanceHistory(employeeId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/month/{employeeId}")
    public ResponseEntity<List<AttendanceDTO>> getMonthly(
            @PathVariable Long employeeId,
            @RequestParam int month,
            @RequestParam int year
    ) {
        List<AttendanceDTO> list = attendanceService.getAttendanceOfMonth(employeeId, month, year);
        return ResponseEntity.ok(list);
    }

    // Admin endpoints
    @GetMapping("/admin/all")
    public ResponseEntity<List<AttendanceDTO>> adminFindAll() {
        return ResponseEntity.ok(attendanceService.adminFindAll());
    }

    @PostMapping("/admin")
    public ResponseEntity<AttendanceDTO> adminCreate(@RequestBody AttendanceDTO dto) {
        return ResponseEntity.ok(attendanceService.adminCreate(dto));
    }

    @PutMapping("/admin/{id}")
    public ResponseEntity<AttendanceDTO> adminUpdate(@PathVariable Long id, @RequestBody AttendanceDTO dto) {
        return ResponseEntity.ok(attendanceService.adminUpdate(id, dto));
    }

    @DeleteMapping("/admin/{id}")
    public ResponseEntity<Void> adminDelete(@PathVariable Long id) {
        attendanceService.adminDelete(id);
        return ResponseEntity.noContent().build();
    }

    // Excel Import/Export endpoints
    @GetMapping("/admin/export")
    public ResponseEntity<ByteArrayResource> exportAttendance(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long employeeId
    ) {
        List<AttendanceDTO> attendances;
        
        if (employeeId != null && month != null && year != null) {
            // Export for specific employee and month
            attendances = attendanceService.getAttendanceOfMonth(employeeId, month, year);
        } else if (month != null && year != null) {
            // Export for all employees in specific month
            attendances = attendanceService.getAttendanceOfMonthForAll(month, year);
        } else {
            // Export all attendance
            attendances = attendanceService.adminFindAll();
        }

        byte[] excelData = excelService.exportAttendanceToExcel(attendances);
        
        ByteArrayResource resource = new ByteArrayResource(excelData);
        
        String filename = "cham_cong_" + 
                (year != null ? year : LocalDate.now().getYear()) + "_" +
                (month != null ? String.format("%02d", month) : "all") + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(excelData.length)
                .body(resource);
    }

    @PostMapping("/admin/import")
    public ResponseEntity<?> importAttendance(@RequestParam("file") MultipartFile file) {
        try {
            if (!file.getOriginalFilename().endsWith(".xlsx") && 
                !file.getOriginalFilename().endsWith(".xls") && 
                !file.getOriginalFilename().endsWith(".csv")) {
                return ResponseEntity.badRequest().body("Chỉ chấp nhận file Excel (.xlsx, .xls) hoặc CSV (.csv)");
            }

            List<AttendanceDTO> attendances = excelService.importAttendanceFromExcel(file);
            
            // Save imported attendances
            List<AttendanceDTO> savedAttendances = new ArrayList<>();
            for (AttendanceDTO dto : attendances) {
                try {
                    AttendanceDTO saved = attendanceService.adminCreate(dto);
                    savedAttendances.add(saved);
                } catch (Exception e) {
                    // Continue with other records if one fails
                    continue;
                }
            }

            return ResponseEntity.ok(new ImportResult(savedAttendances.size(), attendances.size()));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi khi import file: " + e.getMessage());
        }
    }

    // Template download endpoint
    @GetMapping("/admin/template")
    public ResponseEntity<ByteArrayResource> downloadTemplate() {
        // Create empty template
        List<AttendanceDTO> template = new ArrayList<>();
        byte[] templateData = excelService.exportAttendanceToExcel(template);
        
        ByteArrayResource resource = new ByteArrayResource(templateData);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=template_cham_cong.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(templateData.length)
                .body(resource);
    }

    // Helper class for import result
    public static class ImportResult {
        private int imported;
        private int total;

        public ImportResult(int imported, int total) {
            this.imported = imported;
            this.total = total;
        }

        public int getImported() { return imported; }
        public int getTotal() { return total; }
    }
}
