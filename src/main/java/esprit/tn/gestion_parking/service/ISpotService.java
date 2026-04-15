package esprit.tn.gestion_parking.service;

import esprit.tn.gestion_parking.dto.SpotDTO;
import esprit.tn.gestion_parking.entity.Spot;
import esprit.tn.gestion_parking.entity.StatutSpot;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ISpotService {
    SpotDTO addSpot(SpotDTO spotDTO);
    SpotDTO updateSpot(String id, SpotDTO spotDTO);
    List<SpotDTO> getAllSpots();
    SpotDTO getById(String id);
    void deleteSpot(String id);
    List<SpotDTO> findByParking(String parkingId);
    List<SpotDTO> findAvailableByParking(String parkingId);
    void updateStatus(String id, StatutSpot newStatus);
    List<SpotDTO> addMultipleSpots(List<SpotDTO> spots);
    List<SpotDTO> scanAndGenerateSpots(String parkingId, MultipartFile file) throws IOException;
}