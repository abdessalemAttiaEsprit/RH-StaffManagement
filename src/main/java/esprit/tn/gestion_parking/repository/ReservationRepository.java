package esprit.tn.gestion_parking.repository;

import esprit.tn.gestion_parking.entity.Reservation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReservationRepository extends MongoRepository<Reservation, String> {

    // 1. Trouver toutes les réservations d'un Spot spécifique
    // Très important pour vérifier si un spot est libre avant de réserver
    List<Reservation> findBySpotId(String spotId);

    // 2. Trouver les réservations par matricule (ignorer la casse)
    List<Reservation> findByMatriculeIgnoreCase(String matricule);

    // 3. Récupérer uniquement les réservations non supprimées
    List<Reservation> findByIsDeletedFalse();

    // 4. Trouver les réservations d'un spot qui ne sont pas supprimées
    List<Reservation> findBySpotIdAndIsDeletedFalse(String spotId);
}