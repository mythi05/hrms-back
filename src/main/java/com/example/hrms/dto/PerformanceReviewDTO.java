package com.example.hrms.dto;

import com.example.hrms.entity.PerformanceReview.ReviewStatus;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceReviewDTO {

    private Long id;
    private Long employeeId;
    private String employeeName;

    private Long reviewerId;
    private String reviewerName;

    private String period;
    private String goals;
    private Integer score;
    private String comments;
    private ReviewStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
