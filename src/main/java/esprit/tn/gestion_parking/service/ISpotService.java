package esprit.tn.gestion_parking.service;

import esprit.tn.gestion_parking.entity.Spot;
import esprit.tn.gestion_parking.entity.StatutSpot;
import java.util.List;

public interface ISpotService {
    // CRUD
    Spot addSpot(Spot spot);
    Spot updateSpot(String id, Spot spot);
    List<Spot> getAllSpots();
    Spot getById(String id);
    void deleteSpot(String id);

    // Business Logic
    List<Spot> findByParking(String parkingId);
    List<Spot> findAvailableByParking(String parkingId);
    void updateStatus(String id, StatutSpot newStatus);
}