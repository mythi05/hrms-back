package com.example.hrms.controller;

import com.example.hrms.entity.Department;
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
@CrossOrigin(origins = "*", maxAge = 3600)
public class DepartmentController {

    private final DepartmentService departmentService;

    // Lấy tất cả phòng ban
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Department>> getAllDepartments() {
        List<Department> departments = departmentService.getAllDepartments();
        return ResponseEntity.ok(departments);
    }

    // Lấy phòng ban theo ID
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Department> getDepartmentById(@PathVariable Long id) {
        Department department = departmentService.getDepartmentById(id);
        return ResponseEntity.ok(department);
    }

    // Tạo phòng ban mới
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Department> createDepartment(@RequestBody Department department) {
        Department newDepartment = departmentService.createDepartment(department);
        return ResponseEntity.ok(newDepartment);
    }

    // Cập nhật phòng ban
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Department> updateDepartment(@PathVariable Long id, @RequestBody Department departmentDetails) {
        Department updatedDepartment = departmentService.updateDepartment(id, departmentDetails);
        return ResponseEntity.ok(updatedDepartment);
    }

    // Xóa phòng ban
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.ok(Map.of("message", "Department deleted successfully"));
    }

    // Lấy thống kê phòng ban
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getDepartmentStats() {
        Map<String, Object> stats = departmentService.getDepartmentStats();
        return ResponseEntity.ok(stats);
    }

    // Lấy nhân viên trong phòng ban
    @GetMapping("/{id}/employees")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getDepartmentEmployees(@PathVariable Long id) {
        var employees = departmentService.getDepartmentEmployees(id);
        return ResponseEntity.ok(employees);
    }

    // Lấy danh sách phòng ban với số lượng nhân viên
    @GetMapping("/with-count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getDepartmentsWithEmployeeCount() {
        List<Map<String, Object>> departments = departmentService.getDepartmentsWithEmployeeCount();
        return ResponseEntity.ok(departments);
    }
}
