package esprit.tn.gestion_parking.controller;

import esprit.tn.gestion_parking.entity.Parking;
import esprit.tn.gestion_parking.service.IParkingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parkings")
@CrossOrigin(origins = "*") // Useful if you have a frontend (Angular/React)
public class ParkingController {

    @Autowired
    private IParkingService parkingService;

    /**
     * GET /api/parkings
     * Returns all non-deleted parkings.
     */
    @GetMapping
    public ResponseEntity<List<Parking>> getAllParkings() {
        List<Parking> parkings = parkingService.findAll();
        return ResponseEntity.ok(parkings);
    }

    /**
     * GET /api/parkings/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Parking> getParkingById(@PathVariable String id) {
        Parking parking = parkingService.findById(id);
        if (parking == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(parking);
    }

    /**
     * POST /api/parkings
     * Create a new parking lot.
     */
    @PostMapping
    public ResponseEntity<Parking> createParking(@RequestBody Parking parking) {
        Parking savedParking = parkingService.addParking(parking);
        return new ResponseEntity<>(savedParking, HttpStatus.CREATED);
    }

    /**
     * PUT /api/parkings/{id}
     * Update an existing parking lot.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Parking> updateParking(@PathVariable String id, @RequestBody Parking parking) {
        Parking updated = parkingService.updateParking(id, parking);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/parkings/{id}
     * Performs a Soft Delete.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteParking(@PathVariable String id) {
        parkingService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/parkings/promotions
     * Custom endpoint for business logic.
     */
    @GetMapping("/promotions")
    public ResponseEntity<List<Parking>> getPromotions() {
        return ResponseEntity.ok(parkingService.findActivePromotions());
    }
}