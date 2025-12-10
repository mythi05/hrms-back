package com.example.hrms.controller;

import com.example.hrms.dto.EmployeeDTO;
import com.example.hrms.entity.Employee;
import com.example.hrms.entity.Role;
import com.example.hrms.entity.Skill;
import com.example.hrms.exception.ResourceNotFoundException;
import com.example.hrms.mapper.EmployeeMapper;
import com.example.hrms.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/employees")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AdminController {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

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
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        Optional.ofNullable(dto.getFullName()).ifPresent(emp::setFullName);
        Optional.ofNullable(dto.getEmail()).ifPresent(emp::setEmail);
        Optional.ofNullable(dto.getPhone()).ifPresent(emp::setPhone);
        Optional.ofNullable(dto.getDob()).ifPresent(emp::setDob);
        Optional.ofNullable(dto.getPosition()).ifPresent(emp::setPosition);
        Optional.ofNullable(dto.getAddress()).ifPresent(emp::setAddress);
        Optional.ofNullable(dto.getDepartment()).ifPresent(emp::setDepartment);
        Optional.ofNullable(dto.getDepartmentId()).ifPresent(emp::setDepartmentId);
        Optional.ofNullable(dto.getStartDate()).ifPresent(emp::setStartDate);
        Optional.ofNullable(dto.getManagerName()).ifPresent(emp::setManagerName);
        Optional.ofNullable(dto.getContractType()).ifPresent(emp::setContractType);
        Optional.ofNullable(dto.getContractEndDate()).ifPresent(emp::setContractEndDate);
        if (dto.getExperienceYears() > 0) emp.setExperienceYears(dto.getExperienceYears());
        Optional.ofNullable(dto.getGrade()).ifPresent(emp::setGrade);
        if (dto.getPerformanceRate() > 0) emp.setPerformanceRate(dto.getPerformanceRate());
        Optional.ofNullable(dto.getEmployeeCode()).ifPresent(emp::setEmployeeCode);
        if (dto.getSalary() > 0) emp.setSalary(dto.getSalary());

        // Update username
        Optional.ofNullable(dto.getUsername()).ifPresent(emp::setUsername);

        // Update role
        Optional.ofNullable(dto.getRole()).ifPresent(r -> emp.setRole(Role.valueOf(r.toUpperCase())));

        // Update password
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            emp.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        // Update skills: KHÔNG thay collection, chỉ clear + add (tránh lỗi orphanRemoval)
        if (emp.getSkills() == null) {
            emp.setSkills(new ArrayList<>());
        }
        emp.getSkills().clear();
        if (dto.getSkills() != null) {
            for (var sDto : dto.getSkills()) {
                Skill skill = EmployeeMapper.toSkill(sDto);
                skill.setEmployee(emp);
                emp.getSkills().add(skill);
            }
        }

        // Update certificates: tương tự, clear + add vào list hiện tại
        if (emp.getCertificates() == null) {
            emp.setCertificates(new ArrayList<>());
        }
        emp.getCertificates().clear();
        if (dto.getCertificates() != null) {
            emp.getCertificates().addAll(dto.getCertificates());
        }

        employeeRepository.save(emp);
        return ResponseEntity.ok(Map.of("message", "Employee updated", "employee", EmployeeMapper.toDTO(emp)));
    }

    // 4️⃣ Lấy danh sách tất cả nhân viên (trả về List<EmployeeDTO> cho frontend)
    @GetMapping
    public ResponseEntity<List<EmployeeDTO>> listEmployees() {
        List<EmployeeDTO> employees = employeeRepository.findAll()
                .stream()
                .map(EmployeeMapper::toDTO)
                .collect(Collectors.toList());
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
