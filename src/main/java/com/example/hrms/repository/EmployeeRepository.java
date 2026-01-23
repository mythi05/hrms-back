package com.example.hrms.repository;

import com.example.hrms.entity.Employee;
import com.example.hrms.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByUsernameAndIdNot(String username, Long id);
    
    @Query("SELECT e.department, COUNT(e) FROM Employee e WHERE e.department IS NOT NULL GROUP BY e.department")
    List<Object[]> getDepartmentDistribution();
    
    @Query("SELECT e FROM Employee e WHERE MONTH(e.dob) = :month")
    List<Employee> findByMonthOfBirth(int month);
    
    List<Employee> findByDepartmentId(Long departmentId);

    List<Employee> findByRole(Role role);

    // Kiểm tra trùng mã nhân viên
    boolean existsByEmployeeCode(String employeeCode);

    // Kiểm tra trùng mã nhân viên khi update (trừ chính nhân viên hiện tại)
    boolean existsByEmployeeCodeAndIdNot(String employeeCode, Long id);
}