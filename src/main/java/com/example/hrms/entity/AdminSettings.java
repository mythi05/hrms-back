package com.example.hrms.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "admin_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminSettings {

    @Id
    private Long id;

    private String companyName;
    private String taxCode;

    @Column(columnDefinition = "TEXT")
    private String companyAddress;

    private String contactEmail;
    private String contactPhone;

    @Builder.Default
    private Boolean twoFactorAuth = false;

    @Builder.Default
    private Integer sessionTimeoutMinutes = 30;

    @Builder.Default
    private Boolean maintenanceMode = false;

    @Builder.Default
    private Boolean autoBackup = true;

    @Builder.Default
    private Boolean adminEmailNotifications = true;
}
