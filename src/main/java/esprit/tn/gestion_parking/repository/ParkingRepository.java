package esprit.tn.gestion_parking.repository;

import esprit.tn.gestion_parking.entity.Parking;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingRepository extends MongoRepository<Parking, String> {

    // 1. Find all parkings that are NOT marked as deleted
    List<Parking> findByIsDeletedFalse();

    // 2. Find a specific parking by ID only if it's not deleted
    Optional<Parking> findByIdAndIsDeletedFalse(String id);


    List<Parking> findByNomContainingIgnoreCase(String nom);
}