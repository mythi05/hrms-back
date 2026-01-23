package com.example.hrms.controller;

import com.example.hrms.dto.EmployeeDTO;
import com.example.hrms.dto.AttendanceDTO;
import com.example.hrms.service.EmployeeService;
import com.example.hrms.service.ExcelService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final ExcelService excelService;

    // Lấy profile của chính user đang login
    @GetMapping("/me")
    public ResponseEntity<EmployeeDTO> getMyProfile(Authentication authentication) {
        String username = authentication.getName(); 
        EmployeeDTO profile = employeeService.getMe(username);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/me")
    public ResponseEntity<EmployeeDTO> updateMyProfile(Authentication authentication, @RequestBody EmployeeDTO dto) {
        String username = authentication.getName();
        EmployeeDTO updated = employeeService.updateMe(username, dto);
        return ResponseEntity.ok(updated);
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EmployeeDTO> updateMyAvatar(Authentication authentication, @RequestParam("file") MultipartFile file) {
        String username = authentication.getName();
        EmployeeDTO updated = employeeService.updateMyAvatar(username, file);
        return ResponseEntity.ok(updated);
    }

    // Lấy employee theo id
    @GetMapping("/{id}")
    public ResponseEntity<EmployeeDTO> getEmployeeById(@PathVariable Long id) {
        EmployeeDTO employee = employeeService.getById(id);
        return ResponseEntity.ok(employee);
    }

    // Lấy tất cả nhân viên
    @GetMapping
    public ResponseEntity<List<EmployeeDTO>> getAllEmployees() {
        List<EmployeeDTO> employees = employeeService.getAll();
        return ResponseEntity.ok(employees);
    }

    // Tạo mới (Logic kiểm tra trùng mã đã nằm ở Service)
    @PostMapping
    public ResponseEntity<EmployeeDTO> createEmployee(@RequestBody EmployeeDTO dto) {
        EmployeeDTO created = employeeService.create(dto);
        return ResponseEntity.ok(created);
    }

    // Cập nhật (Logic kiểm tra trùng mã đã nằm ở Service)
    @PutMapping("/{id}")
    public ResponseEntity<EmployeeDTO> updateEmployee(@PathVariable Long id, @RequestBody EmployeeDTO dto) {
        EmployeeDTO updated = employeeService.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    @PostMapping(value = "/{id}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EmployeeDTO> updateEmployeeAvatar(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        EmployeeDTO updated = employeeService.updateAvatar(id, file);
        return ResponseEntity.ok(updated);
    }

    // Xóa
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        employeeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Export Excel
    @GetMapping("/admin/export")
    public ResponseEntity<ByteArrayResource> exportEmployees() {
        List<EmployeeDTO> employees = employeeService.getAll();
        byte[] excelData = excelService.exportEmployeesToExcel(employees);
        ByteArrayResource resource = new ByteArrayResource(excelData);
        String filename = "nhan_vien_" + LocalDate.now() + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(excelData.length)
                .body(resource);
    }

    // Import Excel/CSV
    @PostMapping("/admin/import")
    public ResponseEntity<?> importEmployees(@RequestParam("file") MultipartFile file) {
        try {
            if (!file.getOriginalFilename().endsWith(".xlsx") && 
                !file.getOriginalFilename().endsWith(".xls") && 
                !file.getOriginalFilename().endsWith(".csv")) {
                return ResponseEntity.badRequest().body("Chỉ chấp nhận file Excel (.xlsx, .xls) hoặc CSV (.csv)");
            }

            List<EmployeeDTO> employees = excelService.importEmployeesFromExcel(file);
            int savedCount = 0;
            for (EmployeeDTO dto : employees) {
                try {
                    // Gọi service create (nếu trùng mã nó sẽ văng Exception và lọt vào catch bên dưới)
                    employeeService.create(dto);
                    savedCount++;
                } catch (Exception e) {
                    // Bỏ qua dòng bị trùng mã hoặc lỗi định dạng, tiếp tục dòng tiếp theo
                    continue;
                }
            }
            return ResponseEntity.ok(new ImportResult(savedCount, employees.size()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi khi import file: " + e.getMessage());
        }
    }

    @GetMapping("/admin/template")
    public ResponseEntity<ByteArrayResource> downloadEmployeeTemplate() {
        byte[] templateData = excelService.exportEmployeesToExcel(new ArrayList<>());
        ByteArrayResource resource = new ByteArrayResource(templateData);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=template_nhan_vien.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(templateData.length)
                .body(resource);
    }

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