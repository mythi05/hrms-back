package com.example.hrms.repository;

import com.example.hrms.entity.PerformanceReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PerformanceReviewRepository extends JpaRepository<PerformanceReview, Long> {

    List<PerformanceReview> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);

    List<PerformanceReview> findByReviewerIdOrderByCreatedAtDesc(Long reviewerId);

    List<PerformanceReview> findByEmployeeIdAndPeriod(Long employeeId, String period);
}
