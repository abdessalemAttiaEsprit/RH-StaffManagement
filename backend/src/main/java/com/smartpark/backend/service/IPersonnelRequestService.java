package com.smartpark.backend.service;

import com.smartpark.backend.dto.PersonnelRequestDTO;

import java.util.List;

public interface IPersonnelRequestService {
    PersonnelRequestDTO create(String matricule, String message);
    List<PersonnelRequestDTO> findByMatricule(String matricule);

    List<PersonnelRequestDTO> findAll();

    PersonnelRequestDTO updateStatus(String id, String status);
    PersonnelRequestDTO updateMessage(String id, String matricule, String message);
    void delete(String id, String matricule);
}

