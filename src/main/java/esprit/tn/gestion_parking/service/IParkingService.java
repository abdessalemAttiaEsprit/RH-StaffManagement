package esprit.tn.gestion_parking.service;

import esprit.tn.gestion_parking.dto.ParkingDTO;
import esprit.tn.gestion_parking.entity.Parking;
import java.util.List;

public interface IParkingService {
    ParkingDTO addParking(ParkingDTO parkingDTO);
    ParkingDTO updateParking(String id, ParkingDTO parkingDTO);
    List<ParkingDTO> findAll();
    ParkingDTO findById(String id);
    void delete(String id);
    List<ParkingDTO> findActivePromotions();
}