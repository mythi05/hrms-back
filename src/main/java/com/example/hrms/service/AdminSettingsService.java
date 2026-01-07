package com.example.hrms.service;

import com.example.hrms.dto.AdminSettingsDTO;

public interface AdminSettingsService {

    AdminSettingsDTO get();

    AdminSettingsDTO update(AdminSettingsDTO dto);
}
