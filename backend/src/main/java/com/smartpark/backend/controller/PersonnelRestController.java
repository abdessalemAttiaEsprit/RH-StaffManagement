package com.smartpark.backend.controller;
// Importez bien celui-ci pour la manipulation de fichiers
import java.nio.file.Path;
import java.nio.file.Paths;

// Importez ceux-ci pour les ressources Spring
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import com.smartpark.backend.dto.AbsenceDTO;
import com.smartpark.backend.dto.ContractDTO;
import com.smartpark.backend.dto.PersonnelDTO;
import com.smartpark.backend.dto.SalaryAiResponse;
import com.smartpark.backend.service.IPdfService;
import com.smartpark.backend.service.IPersonnelService;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/personnel")
@RequiredArgsConstructor
@CrossOrigin("http://localhost:4200")
public class PersonnelRestController {

    private final IPersonnelService personnelService;
    private final IPdfService pdfService;

    @PostMapping
    public ResponseEntity<PersonnelDTO> create(@Valid @RequestBody PersonnelDTO personnelDTO) {
        return new ResponseEntity<>(personnelService.createPersonnel(personnelDTO), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<PersonnelDTO>> getAll() {
        return ResponseEntity.ok(personnelService.getAllPersonnel());
    }

    @GetMapping("/matricule/{matricule}")
    public ResponseEntity<PersonnelDTO> getByMatricule(@PathVariable String matricule) {
        return ResponseEntity.ok(personnelService.getByMatricule(matricule));
    }

    @PutMapping("/matricule/{matricule}")
    public ResponseEntity<PersonnelDTO> update(@Valid @PathVariable String matricule, @RequestBody PersonnelDTO personnelDTO) {
        return ResponseEntity.ok(personnelService.updatePersonnel(matricule, personnelDTO));
    }

    @PatchMapping("/matricule/{matricule}")
    public ResponseEntity<PersonnelDTO> patchPersonnelInfo(
            @PathVariable String matricule,
            @RequestBody PersonnelDTO partialUpdate) {
        return ResponseEntity.ok(personnelService.updatePersonnelInfo(matricule, partialUpdate));
    }

    @DeleteMapping("/matricule/{matricule}")
    public ResponseEntity<Void> delete(@PathVariable String matricule) {
        personnelService.deletePersonnel(matricule);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/matricule/{matricule}/contract")
    public ResponseEntity<PersonnelDTO> addOrUpdateContract(
            @PathVariable String matricule,
            @Valid @RequestBody ContractDTO contractDTO) {
        return ResponseEntity.ok(personnelService.addContract(matricule, contractDTO));
    }

    @DeleteMapping("/matricule/{matricule}/contract")
    public ResponseEntity<Void> removeContract(@PathVariable String matricule) {
        personnelService.deleteContract(matricule);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/matricule/{matricule}/absences")
    public ResponseEntity<PersonnelDTO> addAbsence(
            @PathVariable String matricule,
            @Valid @RequestBody AbsenceDTO absenceDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(personnelService.addAbsence(matricule, absenceDTO));
    }

    @PutMapping("/matricule/{matricule}/absences/{index}")
    public ResponseEntity<PersonnelDTO> updateAbsence(
            @PathVariable String matricule,
            @PathVariable int index,
            @Valid @RequestBody AbsenceDTO absenceDTO) {
        return ResponseEntity.ok(personnelService.updateAbsence(matricule, index, absenceDTO));
    }

    @DeleteMapping("/matricule/{matricule}/absences/{index}")
    public ResponseEntity<PersonnelDTO> deleteAbsence(
            @PathVariable String matricule,
            @PathVariable int index) {
        personnelService.deleteAbsence(matricule, index);
        return ResponseEntity.ok(personnelService.getByMatricule(matricule));
    }

    @PostMapping("/matricule/{matricule}/absences/{index}/upload")
    public ResponseEntity<PersonnelDTO> uploadJustification(
            @PathVariable String matricule,
            @PathVariable int index,
            @RequestParam("file") MultipartFile file) {
        
        log.info("Endpoint uploadJustification called - matricule: {}, index: {}, filename: {}", matricule, index, file.getOriginalFilename());
        
        try {
            PersonnelDTO updatedPersonnel = personnelService.uploadJustification(matricule, index, file);
            log.info("Upload successful for matricule: {}", matricule);
            return ResponseEntity.ok(updatedPersonnel);
        } catch (Exception e) {
            log.error("Upload failed for matricule: {} - Error: {}", matricule, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/files/{filename:.+}")
    public ResponseEntity<Resource> getFile(@PathVariable String filename) {
        try {
            Path file = Paths.get("uploads").resolve(filename);
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                throw new RuntimeException("Fichier introuvable");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Erreur: " + e.getMessage());
        }
    }

    @GetMapping("/matricule/{matricule}/pdf/contrat")
    public ResponseEntity<?> downloadContractPdf(@PathVariable String matricule) {
        try {
            PersonnelDTO personnel = personnelService.getByMatricule(matricule);
            byte[] pdfBytes = pdfService.generateContratPersonnel(personnel);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Contrat_" + matricule + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(new ByteArrayResource(pdfBytes));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "INTERNAL_SERVER_ERROR", "message", e.getMessage()));
        }
    }

    @GetMapping("/matricule/{matricule}/pdf/attestation")
    public ResponseEntity<?> downloadAttestationPdf(@PathVariable String matricule) {
        try {
            PersonnelDTO personnel = personnelService.getByMatricule(matricule);
            byte[] pdfBytes = pdfService.generateAttestationTravail(personnel);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Attestation_Travail_" + matricule + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(new ByteArrayResource(pdfBytes));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "INTERNAL_SERVER_ERROR", "message", e.getMessage()));
        }
    }

}
