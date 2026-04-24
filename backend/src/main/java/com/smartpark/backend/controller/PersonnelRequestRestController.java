package com.smartpark.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.smartpark.backend.dto.PersonnelRequestDTO;
import com.smartpark.backend.service.IPersonnelRequestService;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/personnel-requests")
@CrossOrigin("http://localhost:4200")
public class PersonnelRequestRestController {

    private final IPersonnelRequestService service;

    @PostMapping("/matricule/{matricule}")
    public ResponseEntity<PersonnelRequestDTO> create(
            @PathVariable String matricule,
            @RequestBody Map<String, String> payload
    ) {
        String message = payload != null ? payload.get("message") : null;
        return ResponseEntity.ok(service.create(matricule, message));
    }

    @GetMapping("/matricule/{matricule}")
    public ResponseEntity<List<PersonnelRequestDTO>> getByMatricule(@PathVariable String matricule) {
        return ResponseEntity.ok(service.findByMatricule(matricule));
    }

    @GetMapping
    public ResponseEntity<List<PersonnelRequestDTO>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<PersonnelRequestDTO> updateStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> payload
    ) {
        String status = payload != null ? payload.get("status") : null;
        return ResponseEntity.ok(service.updateStatus(id, status));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PersonnelRequestDTO> updateMessage(
            @PathVariable String id,
            @RequestBody Map<String, String> payload
    ) {
        String matricule = payload != null ? payload.get("matricule") : null;
        String message = payload != null ? payload.get("message") : null;
        return ResponseEntity.ok(service.updateMessage(id, matricule, message));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> payload
    ) {
        String matricule = payload != null ? payload.get("matricule") : null;
        service.delete(id, matricule);
        return ResponseEntity.noContent().build();
    }
}

