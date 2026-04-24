package com.smartpark.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.smartpark.backend.dto.CandidateDTO;
import com.smartpark.backend.service.ICandidateService;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/candidates")
@CrossOrigin(origins = "http://localhost:4200")
public class CandidateRestController {
    
    @Autowired
    private ICandidateService candidateService;
    

    @PostMapping
    public ResponseEntity<CandidateDTO> registerCandidate(@RequestBody CandidateDTO candidateDTO) {
        CandidateDTO registered = candidateService.registerCandidate(candidateDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(registered);
    }
    

    @GetMapping
    public ResponseEntity<List<CandidateDTO>> getAllCandidates() {
        List<CandidateDTO> candidates = candidateService.getAllCandidates();
        return ResponseEntity.ok(candidates);
    }
    

    @GetMapping("/{id}")
    public ResponseEntity<CandidateDTO> getCandidateById(@PathVariable String id) {
        CandidateDTO candidate = candidateService.getCandidateById(id);
        return ResponseEntity.ok(candidate);
    }
    

    @GetMapping("/email/{email}")
    public ResponseEntity<CandidateDTO> getByEmail(@PathVariable String email) {
        CandidateDTO candidate = candidateService.findByEmail(email);
        return ResponseEntity.ok(candidate);
    }
    

    @PutMapping("/{id}")
    public ResponseEntity<CandidateDTO> updateCandidate(@PathVariable String id, @RequestBody CandidateDTO candidateDTO) {
        CandidateDTO updated = candidateService.updateCandidate(id, candidateDTO);
        return ResponseEntity.ok(updated);
    }
    

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCandidate(@PathVariable String id) {
        candidateService.deleteCandidate(id);
        return ResponseEntity.noContent().build();
    }
    

    @PostMapping("/search/bySkills")
    public ResponseEntity<List<CandidateDTO>> findBySkills(@RequestBody List<String> skills) {
        List<CandidateDTO> candidates = candidateService.findBySkills(skills);
        return ResponseEntity.ok(candidates);
    }
    

    @PostMapping("/{candidateId}/upload-cv")
    public ResponseEntity<CandidateDTO> uploadCv(@PathVariable String candidateId,
                                                  @RequestParam("file") MultipartFile file) {
        try {
            CandidateDTO updated = candidateService.uploadCv(candidateId, file);
            return ResponseEntity.ok(updated);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    

    @GetMapping("/{candidateId}/download-cv")
    public ResponseEntity<?> downloadCv(@PathVariable String candidateId) {
        try {
            byte[] fileContent = candidateService.downloadCv(candidateId);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"cv_" + candidateId + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(fileContent);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{candidateId}/delete-cv")
    public ResponseEntity<Void> deleteCv(@PathVariable String candidateId) {
        candidateService.deleteCv(candidateId);
        return ResponseEntity.noContent().build();
    }
}

