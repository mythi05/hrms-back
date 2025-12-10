package com.example.hrms.repository;

import com.example.hrms.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    
    Optional<Department> findByName(String name);
    
    boolean existsByNameAndIdNot(String name, Long id);
    
    @Query("SELECT d FROM Department d WHERE d.managerId IS NOT NULL")
    List<Department> findDepartmentsWithManager();
    
    @Query("SELECT COUNT(d) FROM Department d")
    Long getTotalDepartments();
    
    List<Department> findByManagerId(Long managerId);
}
