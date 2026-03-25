package esprit.tn.gestion_parking.service;

import esprit.tn.gestion_parking.entity.Reservation;
import java.time.LocalDateTime;
import java.util.List;

public interface IReservationService {
    // Basic CRUD
    Reservation createReservation(Reservation reservation);
    List<Reservation> getAll();
    Reservation getById(String id);
    void cancelReservation(String id);

    // Business Logic
    List<Reservation> findBySpot(String spotId);
    List<Reservation> findByVehicle(String matricule);
    boolean isSpotAvailable(String spotId, LocalDateTime start, LocalDateTime end);
    double calculatePrice(LocalDateTime start, LocalDateTime end);
}