package com.example.hrms.mapper;

import com.example.hrms.dto.PerformanceReviewDTO;
import com.example.hrms.entity.Employee;
import com.example.hrms.entity.PerformanceReview;

public class PerformanceReviewMapper {

    public static PerformanceReviewDTO toDTO(PerformanceReview entity, Employee employee, Employee reviewer) {
        if (entity == null) return null;
        PerformanceReviewDTO dto = new PerformanceReviewDTO();
        dto.setId(entity.getId());
        dto.setEmployeeId(entity.getEmployeeId());
        dto.setReviewerId(entity.getReviewerId());
        dto.setPeriod(entity.getPeriod());
        dto.setGoals(entity.getGoals());
        dto.setScore(entity.getScore());
        dto.setComments(entity.getComments());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        if (employee != null) {
            dto.setEmployeeName(employee.getFullName());
        }
        if (reviewer != null) {
            dto.setReviewerName(reviewer.getFullName());
        }
        return dto;
    }

    public static PerformanceReview toEntity(PerformanceReviewDTO dto) {
        if (dto == null) return null;
        PerformanceReview entity = new PerformanceReview();
        entity.setId(dto.getId());
        entity.setEmployeeId(dto.getEmployeeId());
        entity.setReviewerId(dto.getReviewerId());
        entity.setPeriod(dto.getPeriod());
        entity.setGoals(dto.getGoals());
        entity.setScore(dto.getScore());
        entity.setComments(dto.getComments());
        entity.setStatus(dto.getStatus());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setUpdatedAt(dto.getUpdatedAt());
        return entity;
    }
}
