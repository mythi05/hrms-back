package com.example.hrms.repository;

import com.example.hrms.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);
    long countByEmployeeIdAndReadFlagFalse(Long employeeId);
}
