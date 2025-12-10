package com.example.hrms.mapper;

import com.example.hrms.dto.NotificationDTO;
import com.example.hrms.entity.Notification;

public class NotificationMapper {

    public static NotificationDTO toDTO(Notification entity) {
        if (entity == null) return null;
        return NotificationDTO.builder()
                .id(entity.getId())
                .employeeId(entity.getEmployeeId())
                .title(entity.getTitle())
                .message(entity.getMessage())
                .type(entity.getType())
                .readFlag(entity.isReadFlag())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
