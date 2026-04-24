package com.smartpark.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.smartpark.backend.repository.ISettingsRepo;
import com.smartpark.backend.model.CompanySettings;

import java.util.List;

@Service
@RequiredArgsConstructor // Génère le constructeur pour les champs 'final'
public class SettingsServiceImpl implements ISettingsService{

    private final  ISettingsRepo settingsRepository;
    private static final String DEFAULT_LOGO = "uploads/22646f4d-fb80-4ba7-b4e3-0428ef932a54.png";
    @Override
    public CompanySettings getSettings() {
        List<CompanySettings> settingsList = settingsRepository.findAll();
        if (settingsList.isEmpty()) {
            // Créer un objet par défaut si rien n'existe en base
            CompanySettings defaultSettings = new CompanySettings();
            defaultSettings.setCompanyName("SMARTPARK");
            defaultSettings.setLogoBase64(DEFAULT_LOGO); // On met le chemin par défaut
            return defaultSettings;
        }
        return settingsList.get(0);
    }
    @Override
    public CompanySettings updateSettings(CompanySettings settings) {
        // On récupère l'existant pour être sûr de n'avoir qu'une seule ligne en base
        List<CompanySettings> settingsList = settingsRepository.findAll();
        if (!settingsList.isEmpty()) {
            settings.setId(settingsList.get(0).getId());
        }
        return settingsRepository.save(settings);
    }
}

