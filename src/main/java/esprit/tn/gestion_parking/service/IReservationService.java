package esprit.tn.gestion_parking.service;

import esprit.tn.gestion_parking.dto.ReservationDTO;
import esprit.tn.gestion_parking.entity.Reservation;
import java.time.LocalDateTime;
import java.util.List;

public interface IReservationService {
    ReservationDTO createReservation(ReservationDTO reservationDTO);
    boolean isSpotAvailable(String spotId, LocalDateTime start, LocalDateTime end);
    double calculatePrice(LocalDateTime start, LocalDateTime end);
    List<ReservationDTO> getAll();
    ReservationDTO getById(String id);
    void cancelReservation(String id);
    List<ReservationDTO> findBySpot(String spotId);
    List<ReservationDTO> findByVehicle(String matricule);
    List<ReservationDTO> findByParking(String parkingId);
    ReservationDTO updateReservation(String id, ReservationDTO updatedReservation);
    ReservationDTO enregistrerPassageAuto(ReservationDTO iaDto);
}