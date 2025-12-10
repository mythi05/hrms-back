package com.example.hrms.dto;

import com.example.hrms.entity.Notification.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    private Long id;
    private Long employeeId;
    private String title;
    private String message;
    private NotificationType type;
    private boolean readFlag;
    private LocalDateTime createdAt;
}
