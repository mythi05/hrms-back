package com.example.hrms.controller;

import com.example.hrms.dto.EmployeeDTO;
import com.example.hrms.entity.Department;
import com.example.hrms.entity.Employee;
import com.example.hrms.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<List<Department>> getAllDepartments() {
        return ResponseEntity.ok(departmentService.getAllDepartments());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<Department> getDepartmentById(@PathVariable Long id) {
        return ResponseEntity.ok(departmentService.getDepartmentById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Department> createDepartment(@RequestBody Department department) {
        return ResponseEntity.ok(departmentService.createDepartment(department));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Department> updateDepartment(
            @PathVariable Long id,
            @RequestBody Department departmentDetails) {
        return ResponseEntity.ok(
            departmentService.updateDepartment(id, departmentDetails)
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.ok(Map.of("message", "Department deleted successfully"));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<Map<String, Object>> getDepartmentStats() {
        return ResponseEntity.ok(departmentService.getDepartmentStats());
    }

    @GetMapping("/{id}/employees")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<java.util.List<EmployeeDTO>> getDepartmentEmployees(@PathVariable Long id) {
        return ResponseEntity.ok(
                departmentService.getDepartmentEmployees(id)
                        .stream()
                        .map(this::toLightEmployeeDTO)
                        .toList()
        );
    }

    private EmployeeDTO toLightEmployeeDTO(Employee e) {
        return EmployeeDTO.builder()
                .id(e.getId())
                .fullName(e.getFullName())
                .username(e.getUsername())
                .employeeCode(e.getEmployeeCode())
                .department(e.getDepartment())
                .departmentId(e.getDepartmentId())
                .position(e.getPosition())
                .role(e.getRole() != null ? e.getRole().name() : null)
                .build();
    }

    @GetMapping("/with-count")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getDepartmentsWithEmployeeCount() {
        return ResponseEntity.ok(
            departmentService.getDepartmentsWithEmployeeCount()
        );
    }
}
