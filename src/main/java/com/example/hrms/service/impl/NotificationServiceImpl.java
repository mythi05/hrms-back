package com.example.hrms.service.impl;

import com.example.hrms.dto.NotificationDTO;
import com.example.hrms.entity.Employee;
import com.example.hrms.entity.Notification;
import com.example.hrms.entity.Notification.NotificationType;
import com.example.hrms.exception.ResourceNotFoundException;
import com.example.hrms.mapper.NotificationMapper;
import com.example.hrms.repository.EmployeeRepository;
import com.example.hrms.repository.NotificationRepository;
import com.example.hrms.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmployeeRepository employeeRepository;

    @Override
    public void createNotification(Long employeeId, String title, String message, NotificationType type) {
        if (employeeId == null) return;
        Notification entity = Notification.builder()
                .employeeId(employeeId)
                .title(title)
                .message(message)
                .type(type)
                .readFlag(false)
                .createdAt(LocalDateTime.now())
                .build();
        notificationRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDTO> getMyNotifications(String username) {
        Employee emp = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        return notificationRepository.findByEmployeeIdOrderByCreatedAtDesc(emp.getId())
                .stream()
                .map(NotificationMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void markAsRead(Long notificationId, String username) {
        Employee emp = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        Notification noti = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!emp.getId().equals(noti.getEmployeeId())) {
            throw new IllegalArgumentException("Not your notification");
        }
        noti.setReadFlag(true);
        notificationRepository.save(noti);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(String username) {
        Employee emp = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        return notificationRepository.countByEmployeeIdAndReadFlagFalse(emp.getId());
    }
}
