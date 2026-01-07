package com.example.hrms.controller;

import com.example.hrms.dto.EmployeeDTO;
import com.example.hrms.entity.Employee;
import com.example.hrms.entity.Role;
import com.example.hrms.mapper.EmployeeMapper;
import com.example.hrms.repository.EmployeeRepository;
import com.example.hrms.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/employees")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AdminController {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmployeeService employeeService;

    // 1️⃣ Tạo admin đầu tiên (tạm public)
    @PostMapping("/init")
    public ResponseEntity<?> initAdmin() {
        if (employeeRepository.findByUsername("admin").isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Admin already exists"));
        }

        Employee admin = Employee.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .fullName("Administrator")
                .email("admin@example.com")
                .role(Role.ADMIN)
                .build();

        employeeRepository.save(admin);
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

        Employee emp = EmployeeMapper.toEntity(dto);
        emp.setPassword(passwordEncoder.encode(dto.getPassword())); // encode password

        // Set employee in each skill if any
        if (emp.getSkills() != null) {
            emp.getSkills().forEach(s -> s.setEmployee(emp));
        }

        employeeRepository.save(emp);
        return ResponseEntity.ok(Map.of("message", "Employee created successfully", "employee", EmployeeMapper.toDTO(emp)));
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
}
