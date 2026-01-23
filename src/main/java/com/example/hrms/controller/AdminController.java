package com.example.hrms.controller;

import com.example.hrms.dto.EmployeeDTO;
import com.example.hrms.entity.Employee;
import com.example.hrms.entity.Role;
import com.example.hrms.repository.EmployeeRepository;
import com.example.hrms.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/employees")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AdminController {

    private final EmployeeRepository employeeRepository;
    private final EmployeeService employeeService;

    // 1️⃣ Tạo admin đầu tiên (tạm public)
    @PostMapping("/init")
    public ResponseEntity<?> initAdmin() {
        if (employeeRepository.findByUsername("admin").isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Admin already exists"));
        }

        Employee admin = Employee.builder()
                .username("admin")
                .password("admin123")
                .fullName("Administrator")
                .email("admin@example.com")
                .employeeCode("ADMIN")
                .role(Role.ADMIN)
                .build();

        employeeService.create(EmployeeDTO.builder()
                .username(admin.getUsername())
                .password(admin.getPassword())
                .fullName(admin.getFullName())
                .email(admin.getEmail())
                .employeeCode(admin.getEmployeeCode())
                .role(admin.getRole().name())
                .build());
        return ResponseEntity.ok(Map.of("message", "Admin account created successfully"));
    }

    // 2️⃣ Tạo nhân viên mới (Admin only)
    @PostMapping
    public ResponseEntity<?> createEmployee(@RequestBody EmployeeDTO dto) {
        if (dto.getUsername() == null || dto.getPassword() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing username or password"));
        }

        if (employeeRepository.findByUsername(dto.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username already exists"));
        }

        EmployeeDTO created = employeeService.create(dto);
        return ResponseEntity.ok(Map.of("message", "Employee created successfully", "employee", created));
    }

    // 3️⃣ Cập nhật nhân viên
    @PutMapping("/{id}")
    public ResponseEntity<?> updateEmployee(@PathVariable Long id, @RequestBody EmployeeDTO dto) {
        EmployeeDTO updated = employeeService.update(id, dto);
        return ResponseEntity.ok(Map.of("message", "Employee updated", "employee", updated));
    }

    // 4️⃣ Lấy danh sách tất cả nhân viên (trả về List<EmployeeDTO> cho frontend)
    @GetMapping
    public ResponseEntity<List<EmployeeDTO>> listEmployees() {
        List<EmployeeDTO> employees = employeeService.getAll();
        return ResponseEntity.ok(employees);
    }

    // 5️⃣ Xóa nhân viên
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEmployee(@PathVariable Long id) {
        if (!employeeRepository.existsById(id)) {
            return ResponseEntity.status(404).body(Map.of("error", "Employee not found"));
        }
        employeeRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Employee deleted"));
    }

    // 6️⃣ Xóa nhiều nhân viên (Admin only)
    @PostMapping("/bulk-delete")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> bulkDeleteEmployees(@RequestBody List<Long> ids) {
        try {
            if (ids == null || ids.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Danh sách ID trống"));
            }

            List<Employee> found = employeeRepository.findAllById(ids);
            if (found.isEmpty()) {
                return ResponseEntity.ok(Map.of("deleted", 0, "skipped", 0));
            }

            List<Long> deletableIds = found.stream()
                    .filter(e -> e.getRole() == null || e.getRole() != Role.ADMIN)
                    .map(Employee::getId)
                    .collect(Collectors.toList());

            if (!deletableIds.isEmpty()) {
                employeeRepository.deleteAllById(deletableIds);
                employeeRepository.flush();
            }

            return ResponseEntity.ok(Map.of(
                    "deleted", deletableIds.size(),
                    "skipped", found.size() - deletableIds.size()
            ));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Lỗi xoá nhiều nhân viên",
                    "message", ex.getMessage()
            ));
        }
    }
}
