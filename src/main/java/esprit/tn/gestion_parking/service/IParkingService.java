package esprit.tn.gestion_parking.service;

import esprit.tn.gestion_parking.entity.Parking;
import java.util.List;

public interface IParkingService {
    // Basic CRUD
    Parking addParking(Parking parking);
    Parking updateParking(String id, Parking parking);
    List<Parking> findAll();
    Parking findById(String id);
    void delete(String id); // This will be a "Soft Delete"

    // Business Logic
    List<Parking> findActivePromotions();
}