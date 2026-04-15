package esprit.tn.gestion_parking.repository;

import esprit.tn.gestion_parking.entity.Remise;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RemiseRepository extends MongoRepository<Remise, String> {

    // 1. Trouver toutes les remises actives d'un parking
    List<Remise> findByParkingIdAndIsDeletedFalse(String parkingId);

    // 2. Trouver la meilleure remise pour un nombre d'heures
    Remise findTopByParkingIdAndIsDeletedFalseAndSeuilHeuresLessThanEqualOrderBySeuilHeuresDesc(String parkingId, double dureeHeures);
}