package com.example.hrms.repository;

import com.example.hrms.entity.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    
    List<LeaveRequest> findByEmployeeId(Long employeeId);
    
    List<LeaveRequest> findByStatus(LeaveRequest.Status status);
    
    @Query("SELECT COUNT(lr) FROM LeaveRequest lr WHERE " +
           "lr.status = 'APPROVED' AND " +
           ":date BETWEEN lr.startDate AND lr.endDate")
    Long countActiveLeaveToday(@Param("date") LocalDate date);
    
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.employeeId = :employeeId " +
           "AND lr.status = :status")
    List<LeaveRequest> findByEmployeeIdAndStatus(
        @Param("employeeId") Long employeeId,
        @Param("status") LeaveRequest.Status status
    );
    
    // Dashboard methods
    Long countByStatus(LeaveRequest.Status status);
    
    @Query("SELECT COUNT(lr) FROM LeaveRequest lr WHERE " +
           "lr.status = :status AND " +
           ":startDate BETWEEN lr.startDate AND :endDate")
    Long countByStartDateLessThanEqualAndEndDateGreaterThanEqualAndStatus(
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate, 
        @Param("status") LeaveRequest.Status status
    );
    
    @Query("SELECT SUM(DATEDIFF(lr.endDate, lr.startDate) + 1) FROM LeaveRequest lr " +
           "WHERE lr.employeeId = :employeeId AND lr.status = :status " +
           "AND YEAR(lr.startDate) = :year")
    Long getUsedLeaveDays(@Param("employeeId") Long employeeId, @Param("status") LeaveRequest.Status status, @Param("year") int year);
    
    List<LeaveRequest> findTop10ByOrderByCreatedAtDesc();
}
