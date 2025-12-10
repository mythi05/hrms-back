package com.example.hrms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long employeeId;

    private String title;

    @Column(length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private boolean readFlag;

    private LocalDateTime createdAt;

    public enum NotificationType {
        PAYROLL,
        PAYROLL_UPDATED,
        LEAVE_APPROVED,
        LEAVE_REJECTED,
        TASK_ASSIGNED,
        TASK_UPDATED
    }
}
