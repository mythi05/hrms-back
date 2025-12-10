package com.example.hrms.service;

import com.example.hrms.entity.Department;
import com.example.hrms.entity.Employee;
import com.example.hrms.repository.DepartmentRepository;
import com.example.hrms.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    // Lấy tất cả phòng ban
    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    // Lấy phòng ban theo ID
    public Department getDepartmentById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found with id: " + id));
    }

    // Tạo phòng ban mới
    public Department createDepartment(Department department) {
        // Kiểm tra tên phòng ban đã tồn tại chưa
        if (departmentRepository.findByName(department.getName()).isPresent()) {
            throw new RuntimeException("Department name already exists: " + department.getName());
        }
        
        // Kiểm tra manager có tồn tại không
        if (department.getManagerId() != null) {
            Employee manager = employeeRepository.findById(department.getManagerId())
                    .orElseThrow(() -> new RuntimeException("Manager not found with id: " + department.getManagerId()));
        }
        
        return departmentRepository.save(department);
    }

    // Cập nhật phòng ban
    public Department updateDepartment(Long id, Department departmentDetails) {
        Department existingDepartment = getDepartmentById(id);
        
        // Kiểm tra tên phòng ban đã tồn tại chưa (trừ phòng ban hiện tại)
        if (!existingDepartment.getName().equals(departmentDetails.getName())) {
            if (departmentRepository.findByName(departmentDetails.getName()).isPresent()) {
                throw new RuntimeException("Department name already exists: " + departmentDetails.getName());
            }
        }
        
        // Kiểm tra manager có tồn tại không
        if (departmentDetails.getManagerId() != null) {
            Employee manager = employeeRepository.findById(departmentDetails.getManagerId())
                    .orElseThrow(() -> new RuntimeException("Manager not found with id: " + departmentDetails.getManagerId()));
        }
        
        existingDepartment.setName(departmentDetails.getName());
        existingDepartment.setDescription(departmentDetails.getDescription());
        existingDepartment.setManagerId(departmentDetails.getManagerId());
        
        return departmentRepository.save(existingDepartment);
    }

    // Xóa phòng ban
    public void deleteDepartment(Long id) {
        Department department = getDepartmentById(id);
        
        // Kiểm tra có nhân viên nào trong phòng ban không
        List<Employee> employees = employeeRepository.findByDepartmentId(id);
        if (!employees.isEmpty()) {
            throw new RuntimeException("Cannot delete department with existing employees. Please reassign or remove employees first.");
        }
        
        departmentRepository.delete(department);
    }

    // Lấy thống kê phòng ban
    public Map<String, Object> getDepartmentStats() {
        List<Department> departments = getAllDepartments();
        List<Employee> allEmployees = employeeRepository.findAll();
        
        Map<String, Object> stats = Map.of(
            "totalDepartments", departments.size(),
            "departmentsWithManager", departments.stream().filter(d -> d.getManagerId() != null).count(),
            "totalEmployees", allEmployees.size(),
            "unassignedEmployees", allEmployees.stream().filter(e -> e.getDepartmentId() == null).count()
        );
        
        return stats;
    }

    // Lấy nhân viên trong phòng ban
    public List<Employee> getDepartmentEmployees(Long departmentId) {
        return employeeRepository.findByDepartmentId(departmentId);
    }

    // Lấy danh sách phòng ban với số lượng nhân viên
    public List<Map<String, Object>> getDepartmentsWithEmployeeCount() {
        List<Department> departments = getAllDepartments();
        
        return departments.stream().map(dept -> {
            List<Employee> employees = employeeRepository.findByDepartmentId(dept.getId());
            Map<String, Object> deptMap = new HashMap<>();
            deptMap.put("id", dept.getId());
            deptMap.put("name", dept.getName());
            deptMap.put("description", dept.getDescription() != null ? dept.getDescription() : "");
            deptMap.put("managerId", dept.getManagerId());
            deptMap.put("employeeCount", employees.size());
            return deptMap;
        }).collect(Collectors.toList());
    }
}
