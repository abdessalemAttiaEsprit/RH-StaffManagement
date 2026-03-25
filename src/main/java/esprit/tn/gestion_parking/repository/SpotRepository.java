package esprit.tn.gestion_parking.repository;

import esprit.tn.gestion_parking.entity.Spot;
import esprit.tn.gestion_parking.entity.StatutSpot;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SpotRepository extends MongoRepository<Spot, String> {

    // 1. Trouver tous les spots d'un parking spécifique
    // Spring Data comprend automatiquement qu'il doit chercher l'ID dans l'objet Parking
    List<Spot> findByParkingId(String parkingId);

    // 2. Trouver les spots par statut (LIBRE, RESERVE, MAINTENANCE)
    List<Spot> findByStatut(StatutSpot statut);

    // 3. Trouver les spots d'un parking avec un statut spécifique et non supprimés
    // Utile pour afficher uniquement les places disponibles à la réservation
    List<Spot> findByParkingIdAndStatutAndIsDeletedFalse(String parkingId, StatutSpot statut);

    // 4. Liste simple des spots non supprimés
    List<Spot> findByIsDeletedFalse();
}