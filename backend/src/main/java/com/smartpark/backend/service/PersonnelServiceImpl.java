package com.smartpark.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.smartpark.backend.repository.IPersonnelRepo;
import com.smartpark.backend.repository.IPersonnelRequestRepo;
import com.smartpark.backend.dto.AbsenceDTO;
import com.smartpark.backend.dto.ContractDTO;
import com.smartpark.backend.dto.PersonnelDTO;
import com.smartpark.backend.model.Absence;
import com.smartpark.backend.model.Personnel;
import com.smartpark.backend.exceptions.ResourceNotFoundException;
import com.smartpark.backend.mapper.IAbsenceMapper;
import com.smartpark.backend.mapper.IContractMapper;
import com.smartpark.backend.mapper.IPersonnelMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonnelServiceImpl implements IPersonnelService {

    private final IPersonnelRepo repository;
    private final IPersonnelRequestRepo personnelRequestRepo;
    private final IPersonnelMapper mapper;
    private final IContractMapper contractMapper;
    private final IAbsenceMapper absenceMapper;
    private final AbsenceQuotaCalculator quotaCalculator;


    private String generateUniqueMatricule() {
        String year = String.valueOf(LocalDate.now().getYear());
        // On utilise count + 1 mais on pourrait ajouter un timestamp pour plus de sécurité
        long count = repository.count();
        String candidate = String.format("P-%s-%04d", year, count + 1);

        // Petite sécurité : si par malchance le matricule existe (ex: après suppression)
        // on incrémente jusqu'à trouver un matricule libre
        int suffix = 1;
        while(repository.existsByMatricule(candidate)) {
            candidate = String.format("P-%s-%04d", year, count + 1 + suffix);
            suffix++;
        }
        return candidate;
    }
    @Override
    public PersonnelDTO createPersonnel(PersonnelDTO dto) {
        if (repository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email already in use: " + dto.getEmail());
        }
        Personnel entity = mapper.toEntity(dto);

        // 2. Génération du Matricule Unique
        // On s'assure que même en cas d'id null, le matricule est généré
        entity.setMatricule(generateUniqueMatricule());
        if (entity.getAbsences() == null) {
            entity.setAbsences(new ArrayList<>());
        }
        PersonnelDTO saved = mapper.toDto(repository.save(entity));
        return withQuota(saved);
    }

    @Override
    public PersonnelDTO updatePersonnel(String matricule, PersonnelDTO personnelDTO) {
        Personnel existingPersonnel = repository.findByMatricule(matricule)
                .orElseThrow(() -> new ResourceNotFoundException("Personnel not found with matricule: " + matricule));

        mapper.updatePersonnelFromDto(personnelDTO, existingPersonnel);
        PersonnelDTO saved = mapper.toDto(repository.save(existingPersonnel));
        return withQuota(saved);
    }


    @Override
    public List<PersonnelDTO> getAllPersonnel() {
        return repository.findAll().stream()
                .map(mapper::toDto)
                .map(this::withQuota)
                .collect(Collectors.toList());
    }

    @Override
    public void deletePersonnel(String matricule) {
        if (matricule == null || matricule.isBlank()) {
            throw new IllegalArgumentException("Matricule est obligatoire");
        }

        // Cascade delete: supprimer aussi les demandes associées à cet employé
        personnelRequestRepo.deleteByMatricule(matricule);

        repository.deleteByMatricule(matricule);
    }

    @Override
    public PersonnelDTO updateContract(String matricule, ContractDTO contractDTO) {
        Personnel personnel = repository.findByMatricule(matricule)
                .orElseThrow(() -> new ResourceNotFoundException("Personnel not found with matricule: " + matricule));

        personnel.setContrat(contractMapper.toEntity(contractDTO));
        PersonnelDTO saved = mapper.toDto(repository.save(personnel));
        return withQuota(saved);
    }

    @Override
    public PersonnelDTO addContract(String matricule, ContractDTO contractDTO) {
        return updateContract(matricule, contractDTO);
    }

    @Override
    public PersonnelDTO addAbsence(String matricule, AbsenceDTO absenceDTO) {
        Personnel p = repository.findByMatricule(matricule)
                .orElseThrow(() -> new ResourceNotFoundException("Personnel not found with matricule: " + matricule));

        Absence newAbsence = absenceMapper.toEntity(absenceDTO);

        if (p.getAbsences() == null) {
            p.setAbsences(new ArrayList<>());
        }

        p.getAbsences().add(newAbsence);
        PersonnelDTO saved = mapper.toDto(repository.save(p));
        return withQuota(saved);
    }

    @Override
    public PersonnelDTO updateAbsence(String matricule, int index, AbsenceDTO dto) {
        Personnel p = repository.findByMatricule(matricule)
                .orElseThrow(() -> new ResourceNotFoundException("Personnel not found with matricule: " + matricule));

        if (p.getAbsences() != null && index >= 0 && index < p.getAbsences().size()) {
            // On récupère l'absence existante dans la liste
            Absence existingAbsence = p.getAbsences().get(index);

            // Mise à jour des champs (on suppose que votre DTO a ces nouveaux champs)
            // Note: Assurez-vous que votre entité Absence possède bien ces setters
            existingAbsence.setStartDate(dto.getStartDate());
            existingAbsence.setEndDate(dto.getEndDate());
            existingAbsence.setTypeAbsence(dto.getTypeAbsence());
            existingAbsence.setStatus(dto.getStatus());

            // La justification n'est pas modifiée ici pour ne pas perdre le fichier uploadé

        } else {
            throw new IllegalArgumentException("Invalid absence index: " + index);
        }

        PersonnelDTO saved = mapper.toDto(repository.save(p));
        return withQuota(saved);
    }

    @Override
    public void deleteAbsence(String matricule, int index) {
        Personnel p = repository.findByMatricule(matricule)
                .orElseThrow(() -> new ResourceNotFoundException("Personnel not found with matricule: " + matricule));

        if (p.getAbsences() != null && index >= 0 && index < p.getAbsences().size()) {
            p.getAbsences().remove(index);
            repository.save(p);
        } else {
            throw new IllegalArgumentException("Invalid absence index: " + index);
        }
    }
    private final Path root = Paths.get(System.getProperty("user.dir")).resolve("uploads");
    
    @PostConstruct
    public void init() {
        try {
            log.info("=== INITIALIZING STORAGE ===");
            log.info("Working directory: {}", System.getProperty("user.dir"));
            log.info("Root path for uploads: {}", this.root.toAbsolutePath());
            
            if (!Files.exists(root)) {
                Files.createDirectories(root);
                log.info("✓ Dossier 'uploads' créé à: {}", this.root.toAbsolutePath());
            } else {
                log.info("✓ Dossier 'uploads' existe déjà à: {}", this.root.toAbsolutePath());
            }
            
            // Vérifier les permissions
            log.info("Permissions - Read: {}, Write: {}, Execute: {}", 
                Files.isReadable(root), Files.isWritable(root), Files.isExecutable(root));
            
            log.info("=== STORAGE INITIALIZED ===");
        } catch (IOException e) {
            log.error("✗ ERROR initializing storage: {}", e.getMessage(), e);
            throw new IllegalStateException("Impossible d'initialiser le dossier de stockage: " + e.getMessage());
        }
    }

    @Override
    public PersonnelDTO uploadJustification(String matricule, int index, MultipartFile file) {
        log.info("=== UPLOAD JUSTIFICATION START ===");
        log.info("Matricule: {}, Index: {}", matricule, index);
        log.info("File - Name: {}, Size: {} bytes, ContentType: {}", 
            file.getOriginalFilename(), file.getSize(), file.getContentType());
        
        try {
            // Vérifier que le fichier n'est pas vide
            if (file.isEmpty()) {
                log.error("✗ File is empty");
                throw new IllegalArgumentException("Le fichier est vide");
            }

            log.info("Uploads directory: {}", this.root.toAbsolutePath());

            // Récupérer le personnel
            Personnel personnel = repository.findByMatricule(matricule)
                    .orElseThrow(() -> {
                        log.error("✗ Personnel not found with matricule: {}", matricule);
                        return new ResourceNotFoundException("Personnel not found with matricule: " + matricule);
                    });
            
            log.info("✓ Personnel found: {} {}", personnel.getNom(), personnel.getPrenom());

            // Vérifier que l'index est valide
            if (personnel.getAbsences() == null || index < 0 || index >= personnel.getAbsences().size()) {
                int absenceCount = personnel.getAbsences() != null ? personnel.getAbsences().size() : 0;
                log.error("✗ Invalid absence index. Index: {}, Total absences: {}", index, absenceCount);
                throw new IllegalArgumentException("Invalid absence index: " + index);
            }
            
            log.info("✓ Valid absence index");

            // Nettoyer le nom du fichier
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path targetPath = this.root.resolve(fileName);
            
            log.info("Writing file to: {}", targetPath.toAbsolutePath());

            // Copier le fichier
            Files.copy(file.getInputStream(), targetPath);
            
            log.info("✓ File written successfully");
            log.info("✓ File exists: {}", Files.exists(targetPath));
            log.info("✓ File size on disk: {} bytes", Files.size(targetPath));

            // Mettre à jour la BD
            personnel.getAbsences().get(index).setJustification(fileName);
            repository.save(personnel);
            
            log.info("✓ Database updated with filename: {}", fileName);
            log.info("=== UPLOAD JUSTIFICATION SUCCESS ===");

            return withQuota(mapper.toDto(personnel));
            
        } catch (ResourceNotFoundException e) {
            log.error("✗ ResourceNotFoundException: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("✗ IllegalArgumentException: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("✗ Exception during upload: {}", e.getMessage(), e);
            throw new IllegalStateException("Erreur lors de l'enregistrement du fichier: " + e.getMessage());
        }
    }
    @Override
    public void deleteContract(String matricule) {
        Personnel p = repository.findByMatricule(matricule)
                .orElseThrow(() -> new ResourceNotFoundException("Personnel not found with matricule: " + matricule));

        p.setContrat(null);
        repository.save(p);
    }
    @Override
    public PersonnelDTO getByMatricule(String matricule) {
        Personnel personnel = repository.findByMatricule(matricule)
                .orElseThrow(() -> new ResourceNotFoundException("Personnel with matricule '" + matricule + "' not found"));

        return withQuota(mapper.toDto(personnel)); // Conversion + quota
    }

    @Override
    public PersonnelDTO updatePersonnelInfo(String matricule, PersonnelDTO partialUpdate) {
        Personnel personnel = repository.findByMatricule(matricule)
                .orElseThrow(() -> new ResourceNotFoundException("Personnel not found with matricule: " + matricule));

        // Mise à jour partielle : RIB et CNSS uniquement
        if (partialUpdate.getRib() != null) {
            personnel.setRib(partialUpdate.getRib());
        }
        if (partialUpdate.getCnssNumber() != null) {
            personnel.setCnssNumber(partialUpdate.getCnssNumber());
        }

        PersonnelDTO saved = mapper.toDto(repository.save(personnel));
        return withQuota(saved);
    }

    private PersonnelDTO withQuota(PersonnelDTO dto) {
        if (dto == null) return null;
        // Recharger l'entité si besoin de l'historique complet (absences + contrat)
        // Ici, dto contient déjà absences+contrat via mapper, donc on s'appuie dessus.
        // Pour un calcul fiable, on refait une lecture DB par matricule.
        if (dto.getMatricule() == null || dto.getMatricule().isBlank()) {
            return dto;
        }
        Personnel p = repository.findByMatricule(dto.getMatricule()).orElse(null);
        if (p == null) {
            return dto;
        }
        AbsenceQuotaCalculator.QuotaSnapshot snap = quotaCalculator.computeAsOf(p, LocalDate.now());
        dto.setAbsenceQuotaMonthlyDays(snap.monthlyQuotaDays());
        dto.setAbsenceQuotaEarnedDays(snap.earnedDays());
        dto.setAbsenceQuotaUsedJustifiedDays(snap.usedJustifiedDays());
        dto.setAbsenceQuotaRemainingDays(snap.remainingDays());
        return dto;
    }

}

