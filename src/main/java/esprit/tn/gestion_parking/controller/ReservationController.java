package esprit.tn.gestion_parking.controller;

import esprit.tn.gestion_parking.entity.Reservation;
import esprit.tn.gestion_parking.service.IReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@CrossOrigin(origins = "*")
public class ReservationController {

    @Autowired
    private IReservationService reservationService;

    /**
     * GET /api/reservations
     * List all active (not cancelled) reservations.
     */
    @GetMapping
    public ResponseEntity<List<Reservation>> getAll() {
        return ResponseEntity.ok(reservationService.getAll());
    }

    /**
     * GET /api/reservations/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Reservation> getById(@PathVariable String id) {
        Reservation res = reservationService.getById(id);
        return (res != null) ? ResponseEntity.ok(res) : ResponseEntity.notFound().build();
    }

    /**
     * POST /api/reservations
     * Main booking endpoint. Handles price calculation and availability check.
     */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Reservation reservation) {
        try {
            Reservation saved = reservationService.createReservation(reservation);
            return new ResponseEntity<>(saved, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            // Handle the "Spot already taken" error from the service
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    /**
     * DELETE /api/reservations/{id}
     * Cancels a reservation (Soft Delete).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable String id) {
        reservationService.cancelReservation(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/reservations/spot/{spotId}
     * Find history/upcoming bookings for a specific spot.
     */
    @GetMapping("/spot/{spotId}")
    public ResponseEntity<List<Reservation>> getBySpot(@PathVariable String spotId) {
        return ResponseEntity.ok(reservationService.findBySpot(spotId));
    }

    /**
     * GET /api/reservations/vehicle/{matricule}
     * Search bookings by license plate.
     */
    @GetMapping("/vehicle/{matricule}")
    public ResponseEntity<List<Reservation>> getByVehicle(@PathVariable String matricule) {
        // Note: You might need to add this method to your IReservationService
        return ResponseEntity.ok(reservationService.findByVehicle(matricule));
    }
}