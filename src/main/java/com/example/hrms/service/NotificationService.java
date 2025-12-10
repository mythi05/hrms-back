package com.example.hrms.service;

import com.example.hrms.dto.NotificationDTO;
import com.example.hrms.entity.Notification.NotificationType;

import java.util.List;

public interface NotificationService {

    void createNotification(Long employeeId, String title, String message, NotificationType type);

    List<NotificationDTO> getMyNotifications(String username);

    void markAsRead(Long notificationId, String username);

    long countUnread(String username);
}
