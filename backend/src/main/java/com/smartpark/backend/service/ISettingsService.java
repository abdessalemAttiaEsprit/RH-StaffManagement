package com.smartpark.backend.service;

import com.smartpark.backend.model.CompanySettings;

public interface ISettingsService {
    CompanySettings getSettings();
    CompanySettings updateSettings(CompanySettings settings);
}

