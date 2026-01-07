package com.example.hrms.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminSettingsDTO {

    private Long id;

    private String companyName;
    private String taxCode;
    private String companyAddress;

    private String contactEmail;
    private String contactPhone;

    private Boolean twoFactorAuth;
    private Integer sessionTimeoutMinutes;

    private Boolean maintenanceMode;
    private Boolean autoBackup;
    private Boolean adminEmailNotifications;
}
