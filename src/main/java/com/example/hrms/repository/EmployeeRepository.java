package com.example.hrms.repository;

import com.example.hrms.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    // Thêm method này để filter có thể tìm Employee theo username
    Optional<Employee> findByUsername(String username);
    
    // Get department distribution
    @Query("SELECT e.department, COUNT(e) FROM Employee e WHERE e.department IS NOT NULL GROUP BY e.department")
    List<Object[]> getDepartmentDistribution();
    
    // Find employees by birth month
    @Query("SELECT e FROM Employee e WHERE MONTH(e.dob) = :month")
    List<Employee> findByMonthOfBirth(int month);
    
    // Find employees by department ID
    List<Employee> findByDepartmentId(Long departmentId);
}
