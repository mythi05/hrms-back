package com.example.hrms.repository;

import com.example.hrms.entity.Payroll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollRepository extends JpaRepository<Payroll, Long> {
    
    List<Payroll> findByEmployeeId(Long employeeId);
    
    List<Payroll> findByMonthAndYear(Integer month, Integer year);
    
    Optional<Payroll> findByEmployeeIdAndMonthAndYear(Long employeeId, Integer month, Integer year);
    
    Boolean existsByEmployeeIdAndMonthAndYear(Long employeeId, Integer month, Integer year);
    
    // Dashboard methods
    @Query("SELECT SUM(p.netSalary), AVG(p.netSalary), COUNT(p) FROM Payroll p " +
           "WHERE p.month = :month AND p.year = :year")
    List<Object[]> getMonthlyPayrollStats(int month, int year);
}
