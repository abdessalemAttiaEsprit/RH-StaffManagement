package com.smartpark.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.smartpark.backend.repository.IPersonnelRepo;
import com.smartpark.backend.repository.IPersonnelRequestRepo;
import com.smartpark.backend.dto.PersonnelRequestDTO;
import com.smartpark.backend.model.Personnel;
import com.smartpark.backend.model.PersonnelRequest;
import com.smartpark.backend.exceptions.ResourceNotFoundException;
import com.smartpark.backend.mapper.IPersonnelRequestMapper;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonnelRequestServiceImpl implements IPersonnelRequestService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_ACCEPTED = "ACCEPTED";
    private static final String STATUS_REJECTED = "REJECTED";

    private final IPersonnelRequestRepo requestRepo;
    private final IPersonnelRepo personnelRepo;
    private final IPersonnelRequestMapper mapper;

    @Override
    public PersonnelRequestDTO create(String matricule, String message) {
        if (matricule == null || matricule.isBlank()) {
            throw new IllegalArgumentException("Matricule est obligatoire");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message est obligatoire");
        }

        Personnel p = personnelRepo.findByMatricule(matricule)
                .orElseThrow(() -> new ResourceNotFoundException("Personnel not found with matricule: " + matricule));

        PersonnelRequest entity = PersonnelRequest.builder()
                .matricule(matricule)
                .fullPersonnelName((p.getNom() != null ? p.getNom() : "") + " " + (p.getPrenom() != null ? p.getPrenom() : ""))
                .message(message.trim())
                .createdAt(LocalDateTime.now())
            .status(STATUS_PENDING)
                .build();

        return mapper.toDto(requestRepo.save(entity));
    }

    @Override
    public List<PersonnelRequestDTO> findByMatricule(String matricule) {
        if (matricule == null || matricule.isBlank()) {
            throw new IllegalArgumentException("Matricule est obligatoire");
        }
        return requestRepo.findByMatriculeOrderByCreatedAtDesc(matricule).stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    public List<PersonnelRequestDTO> findAll() {
        return requestRepo.findAllByOrderByCreatedAtDesc().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    public PersonnelRequestDTO updateStatus(String id, String status) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Id est obligatoire");
        }
        String normalized = status != null ? status.trim().toUpperCase() : "";
        if (!STATUS_ACCEPTED.equals(normalized) && !STATUS_REJECTED.equals(normalized) && !STATUS_PENDING.equals(normalized)) {
            throw new IllegalArgumentException("Statut invalide");
        }

        PersonnelRequest entity = requestRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PersonnelRequest not found with id: " + id));

        entity.setStatus(normalized);
        return mapper.toDto(requestRepo.save(entity));
    }

    @Override
    public PersonnelRequestDTO updateMessage(String id, String matricule, String message) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Id est obligatoire");
        }
        if (matricule == null || matricule.isBlank()) {
            throw new IllegalArgumentException("Matricule est obligatoire");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message est obligatoire");
        }

        PersonnelRequest entity = requestRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PersonnelRequest not found with id: " + id));

        if (!matricule.trim().equalsIgnoreCase(entity.getMatricule())) {
            throw new IllegalArgumentException("Matricule invalide");
        }
        if (!STATUS_PENDING.equalsIgnoreCase(entity.getStatus())) {
            throw new IllegalStateException("Impossible de modifier une demande déjà traitée");
        }

        entity.setMessage(message.trim());
        return mapper.toDto(requestRepo.save(entity));
    }

    @Override
    public void delete(String id, String matricule) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Id est obligatoire");
        }

        PersonnelRequest entity = requestRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PersonnelRequest not found with id: " + id));

        if (matricule != null && !matricule.isBlank()) {
            if (!matricule.trim().equalsIgnoreCase(entity.getMatricule())) {
                throw new IllegalArgumentException("Matricule invalide");
            }
            if (!STATUS_PENDING.equalsIgnoreCase(entity.getStatus())) {
                throw new IllegalStateException("Impossible de supprimer une demande déjà traitée");
            }
        }

        requestRepo.deleteById(entity.getId());
    }
}

