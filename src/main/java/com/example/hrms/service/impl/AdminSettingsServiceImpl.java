package com.example.hrms.service.impl;

import com.example.hrms.dto.AdminSettingsDTO;
import com.example.hrms.entity.AdminSettings;
import com.example.hrms.repository.AdminSettingsRepository;
import com.example.hrms.service.AdminSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminSettingsServiceImpl implements AdminSettingsService {

    private static final Long SINGLETON_ID = 1L;

    private final AdminSettingsRepository repository;

    @Override
    @Transactional(readOnly = true)
    public AdminSettingsDTO get() {
        AdminSettings entity = repository.findById(SINGLETON_ID).orElseGet(() -> {
            AdminSettings created = AdminSettings.builder().id(SINGLETON_ID).build();
            return repository.save(created);
        });
        return toDTO(entity);
    }

    @Override
    @Transactional
    public AdminSettingsDTO update(AdminSettingsDTO dto) {
        AdminSettings entity = repository.findById(SINGLETON_ID).orElseGet(() -> AdminSettings.builder().id(SINGLETON_ID).build());

        if (dto.getCompanyName() != null) entity.setCompanyName(dto.getCompanyName());
        if (dto.getTaxCode() != null) entity.setTaxCode(dto.getTaxCode());
        if (dto.getCompanyAddress() != null) entity.setCompanyAddress(dto.getCompanyAddress());
        if (dto.getContactEmail() != null) entity.setContactEmail(dto.getContactEmail());
        if (dto.getContactPhone() != null) entity.setContactPhone(dto.getContactPhone());

        if (dto.getTwoFactorAuth() != null) entity.setTwoFactorAuth(dto.getTwoFactorAuth());
        if (dto.getSessionTimeoutMinutes() != null) entity.setSessionTimeoutMinutes(dto.getSessionTimeoutMinutes());

        if (dto.getMaintenanceMode() != null) entity.setMaintenanceMode(dto.getMaintenanceMode());
        if (dto.getAutoBackup() != null) entity.setAutoBackup(dto.getAutoBackup());
        if (dto.getAdminEmailNotifications() != null) entity.setAdminEmailNotifications(dto.getAdminEmailNotifications());

        AdminSettings saved = repository.save(entity);
        return toDTO(saved);
    }

    private static AdminSettingsDTO toDTO(AdminSettings e) {
        return AdminSettingsDTO.builder()
                .id(e.getId())
                .companyName(e.getCompanyName())
                .taxCode(e.getTaxCode())
                .companyAddress(e.getCompanyAddress())
                .contactEmail(e.getContactEmail())
                .contactPhone(e.getContactPhone())
                .twoFactorAuth(e.getTwoFactorAuth())
                .sessionTimeoutMinutes(e.getSessionTimeoutMinutes())
                .maintenanceMode(e.getMaintenanceMode())
                .autoBackup(e.getAutoBackup())
                .adminEmailNotifications(e.getAdminEmailNotifications())
                .build();
    }
}
