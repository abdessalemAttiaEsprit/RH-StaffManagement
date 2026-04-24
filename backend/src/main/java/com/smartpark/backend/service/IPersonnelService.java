package com.smartpark.backend.service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import com.smartpark.backend.dto.AbsenceDTO;
import com.smartpark.backend.dto.ContractDTO;
import com.smartpark.backend.dto.PersonnelDTO;

import java.util.List;

public interface IPersonnelService {
    PersonnelDTO createPersonnel(PersonnelDTO personnelDTO);
    PersonnelDTO updatePersonnel(String matricule, PersonnelDTO personnelDTO);
    PersonnelDTO updatePersonnelInfo(String matricule, PersonnelDTO partialUpdate);
    List<PersonnelDTO> getAllPersonnel();
    void deletePersonnel(String matricule);
    PersonnelDTO updateContract (String matricule, ContractDTO contractDTO);
    public void deleteContract(String matricule);
    public PersonnelDTO addContract(String matricule, ContractDTO contractDTO);
    // --- Gestion des Absences ---
    // Correction : Il faut passer l'ID du personnel et le DTO de l'absence
    PersonnelDTO addAbsence(String matricule, AbsenceDTO absenceDTO);
    PersonnelDTO getByMatricule(String matricule);
    // Optionnel : Pour supprimer ou modifier une absence spécifique
    PersonnelDTO updateAbsence(String personnelId, int absenceIndex, AbsenceDTO absenceDTO);
    void deleteAbsence(String personnelId, int absenceIndex);
    PersonnelDTO uploadJustification(String id, int index, MultipartFile file);
}

