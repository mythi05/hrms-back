package com.example.hrms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "performance_reviews")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long employeeId;

    private Long reviewerId; // HR/Admin hoặc quản lý trực tiếp

    private String period; // Ví dụ: "2025-Q1", "2025-H1", "2025"

    @Column(length = 2000)
    private String goals; // Mục tiêu / mô tả

    private Integer score; // Điểm tổng (0-100 hoặc thang 1-5)

    @Column(length = 2000)
    private String comments; // Nhận xét của người đánh giá

    @Enumerated(EnumType.STRING)
    private ReviewStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public enum ReviewStatus {
        DRAFT,
        SUBMITTED,
        APPROVED
    }
}
