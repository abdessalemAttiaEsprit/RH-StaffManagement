package esprit.tn.gestion_parking.controller;

import esprit.tn.gestion_parking.dto.RecetteDTO;
import esprit.tn.gestion_parking.service.IRecetteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recettes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RecetteController {

    private final IRecetteService recetteService;

    @GetMapping("/admin-stats")
    public ResponseEntity<RecetteDTO> getAdminStats() {
        return ResponseEntity.ok(recetteService.getStatistiquesGlobales());
    }
}