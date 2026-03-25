package esprit.tn.gestion_parking.service;

import esprit.tn.gestion_parking.entity.Reservation;
import esprit.tn.gestion_parking.repository.ReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReservationServiceImpl implements IReservationService {

    @Autowired
    private ReservationRepository reservationRepository;

    @Override
    public Reservation createReservation(Reservation res) {
        // 1. Validation: Is the spot free for this specific time?
        if (!isSpotAvailable(res.getSpot().getId(), res.getDatetimeEntree(), res.getDatetimeSortie())) {
            throw new RuntimeException("Conflict: This spot is already reserved for the requested time.");
        }

        // 2. Data Preparation
        res.setCreatedAt(LocalDateTime.now());
        res.setUpdatedAt(LocalDateTime.now());
        res.setDate(LocalDateTime.now());
        res.setIsDeleted(false);
        res.setQrCode(UUID.randomUUID().toString()); // Secure token generation

        // 3. Price Calculation (Auto-calculation if not provided)
        if (res.getMontant() <= 0) {
            res.setMontant(calculatePrice(res.getDatetimeEntree(), res.getDatetimeSortie()));
        }

        return reservationRepository.save(res);
    }

    @Override
    public boolean isSpotAvailable(String spotId, LocalDateTime start, LocalDateTime end) {
        // Find all active reservations for this spot
        List<Reservation> existing = reservationRepository.findBySpotId(spotId);

        // Logic: (StartA < EndB) AND (EndA > StartB) means there is an overlap
        return existing.stream()
                .filter(r -> !r.getIsDeleted())
                .noneMatch(r -> start.isBefore(r.getDatetimeSortie()) && end.isAfter(r.getDatetimeEntree()));
    }

    @Override
    public double calculatePrice(LocalDateTime start, LocalDateTime end) {
        long hours = Duration.between(start, end).toHours();
        if (hours <= 0) hours = 1; // Minimum 1 hour charge
        double hourlyRate = 5.0;    // You could fetch this from the Parking entity
        return hours * hourlyRate;
    }

    @Override
    public List<Reservation> getAll() {
        return reservationRepository.findByIsDeletedFalse();
    }

    @Override
    public Reservation getById(String id) {
        return reservationRepository.findById(id)
                .filter(r -> !r.getIsDeleted())
                .orElse(null);
    }

    @Override
    public void cancelReservation(String id) {
        reservationRepository.findById(id).ifPresent(res -> {
            res.setIsDeleted(true);
            res.setUpdatedAt(LocalDateTime.now());
            reservationRepository.save(res);
        });
    }

    @Override
    public List<Reservation> findBySpot(String spotId) {
        return reservationRepository.findBySpotIdAndIsDeletedFalse(spotId);
    }

    @Override
    public List<Reservation> findByVehicle(String matricule) {
        return reservationRepository.findByMatriculeIgnoreCase(matricule).stream()
                .filter(r -> !r.getIsDeleted())
                .collect(Collectors.toList());
    }
}