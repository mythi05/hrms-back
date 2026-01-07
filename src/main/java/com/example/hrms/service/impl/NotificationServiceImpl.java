package com.example.hrms.service.impl;

import com.example.hrms.dto.NotificationDTO;
import com.example.hrms.entity.Employee;
import com.example.hrms.entity.Notification;
import com.example.hrms.entity.Notification.NotificationType;
import com.example.hrms.entity.Role;
import com.example.hrms.entity.User;
import com.example.hrms.exception.ResourceNotFoundException;
import com.example.hrms.mapper.NotificationMapper;
import com.example.hrms.repository.EmployeeRepository;
import com.example.hrms.repository.NotificationRepository;
import com.example.hrms.repository.UserRepository;
import com.example.hrms.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
    private final UserRepository userRepository;

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
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
        try {
            notificationRepository.save(entity);
        } catch (Exception ex) {
            return;
        }
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void createNotificationForAdmins(String title, String message, NotificationType type) {
        employeeRepository.findByRole(Role.ADMIN)
                .forEach(admin -> createNotification(admin.getId(), title, message, type));

        employeeRepository.findByRole(Role.HR)
                .forEach(hr -> createNotification(hr.getId(), title, message, type));

        userRepository.findByRole(User.Role.ADMIN)
                .stream()
                .map(User::getEmployeeId)
                .filter(id -> id != null)
                .distinct()
                .forEach(adminEmployeeId -> createNotification(adminEmployeeId, title, message, type));

        userRepository.findByRole(User.Role.HR)
                .stream()
                .map(User::getEmployeeId)
                .filter(id -> id != null)
                .distinct()
                .forEach(hrEmployeeId -> createNotification(hrEmployeeId, title, message, type));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDTO> getMyNotifications(String username) {
        Long employeeId = employeeRepository.findByUsername(username)
                .map(Employee::getId)
                .orElseGet(() -> userRepository.findByUsername(username)
                        .map(User::getEmployeeId)
                        .orElse(null));

        if (employeeId == null) {
            throw new ResourceNotFoundException("Employee not found");
        }

        return notificationRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId)
                .stream()
                .map(NotificationMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void markAsRead(Long notificationId, String username) {
        Long employeeId = employeeRepository.findByUsername(username)
                .map(Employee::getId)
                .orElseGet(() -> userRepository.findByUsername(username)
                        .map(User::getEmployeeId)
                        .orElse(null));

        if (employeeId == null) {
            throw new ResourceNotFoundException("Employee not found");
        }
        Notification noti = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!employeeId.equals(noti.getEmployeeId())) {
            throw new IllegalArgumentException("Not your notification");
        }
        noti.setReadFlag(true);
        notificationRepository.save(noti);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(String username) {
        Long employeeId = employeeRepository.findByUsername(username)
                .map(Employee::getId)
                .orElseGet(() -> userRepository.findByUsername(username)
                        .map(User::getEmployeeId)
                        .orElse(null));

        if (employeeId == null) {
            throw new ResourceNotFoundException("Employee not found");
        }

        return notificationRepository.countByEmployeeIdAndReadFlagFalse(employeeId);
    }
}
