package esprit.tn.gestion_parking.controller;

import esprit.tn.gestion_parking.entity.Spot;
import esprit.tn.gestion_parking.entity.StatutSpot;
import esprit.tn.gestion_parking.service.ISpotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/spots")
@CrossOrigin(origins = "*")
public class SpotController {

    @Autowired
    private ISpotService spotService;

    /**
     * GET /api/spots
     * List all active spots across all parkings.
     */
    @GetMapping
    public ResponseEntity<List<Spot>> getAll() {
        return ResponseEntity.ok(spotService.getAllSpots());
    }

    /**
     * GET /api/spots/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Spot> getById(@PathVariable String id) {
        Spot spot = spotService.getById(id);
        return (spot != null) ? ResponseEntity.ok(spot) : ResponseEntity.notFound().build();
    }

    /**
     * POST /api/spots
     */
    @PostMapping
    public ResponseEntity<Spot> create(@RequestBody Spot spot) {
        return new ResponseEntity<>(spotService.addSpot(spot), HttpStatus.CREATED);
    }

    /**
     * PUT /api/spots/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Spot> update(@PathVariable String id, @RequestBody Spot spot) {
        Spot updated = spotService.updateSpot(id, spot);
        return (updated != null) ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    /**
     * DELETE /api/spots/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        spotService.deleteSpot(id);
        return ResponseEntity.noContent().build();
    }

    // --- Relationship & Business Logic Endpoints ---

    /**
     * GET /api/spots/parking/{parkingId}
     * Get all spots for a specific parking.
     */
    @GetMapping("/parking/{parkingId}")
    public ResponseEntity<List<Spot>> getByParking(@PathVariable String parkingId) {
        return ResponseEntity.ok(spotService.findByParking(parkingId));
    }

    /**
     * GET /api/spots/parking/{parkingId}/available
     * Get only the FREE spots for a specific parking.
     */
    @GetMapping("/parking/{parkingId}/available")
    public ResponseEntity<List<Spot>> getAvailableByParking(@PathVariable String parkingId) {
        return ResponseEntity.ok(spotService.findAvailableByParking(parkingId));
    }

    /**
     * PATCH /api/spots/{id}/status
     * Partial update to change status (e.g., to MAINTENANCE).
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> changeStatus(@PathVariable String id, @RequestParam StatutSpot status) {
        spotService.updateStatus(id, status);
        return ResponseEntity.ok().build();
    }
}