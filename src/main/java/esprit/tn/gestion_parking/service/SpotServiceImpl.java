package esprit.tn.gestion_parking.service;

import esprit.tn.gestion_parking.entity.Spot;
import esprit.tn.gestion_parking.entity.StatutSpot;
import esprit.tn.gestion_parking.repository.SpotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SpotServiceImpl implements ISpotService {

    @Autowired
    private SpotRepository spotRepository;

    @Override
    public Spot addSpot(Spot spot) {
        spot.setCreatedAt(LocalDateTime.now());
        spot.setUpdatedAt(LocalDateTime.now());
        spot.setIsDeleted(false);
        if (spot.getStatut() == null) {
            spot.setStatut(StatutSpot.LIBRE);
        }
        return spotRepository.save(spot);
    }

    @Override
    public Spot updateSpot(String id, Spot details) {
        return spotRepository.findById(id).map(spot -> {
            spot.setNom(details.getNom());
            spot.setDescription(details.getDescription());
            spot.setStatut(details.getStatut());
            spot.setUpdatedAt(LocalDateTime.now());
            return spotRepository.save(spot);
        }).orElse(null);
    }

    @Override
    public List<Spot> getAllSpots() {
        return spotRepository.findAll().stream()
                .filter(s -> !s.getIsDeleted())
                .collect(Collectors.toList());
    }

    @Override
    public Spot getById(String id) {
        return spotRepository.findById(id)
                .filter(s -> !s.getIsDeleted())
                .orElse(null);
    }

    @Override
    public void deleteSpot(String id) {
        spotRepository.findById(id).ifPresent(spot -> {
            spot.setIsDeleted(true);
            spot.setUpdatedAt(LocalDateTime.now());
            spotRepository.save(spot);
        });
    }

    @Override
    public List<Spot> findByParking(String parkingId) {
        // This requires findByParkingId in your SpotRepository
        return spotRepository.findByParkingId(parkingId).stream()
                .filter(s -> !s.getIsDeleted())
                .collect(Collectors.toList());
    }

    @Override
    public List<Spot> findAvailableByParking(String parkingId) {
        return spotRepository.findByParkingId(parkingId).stream()
                .filter(s -> !s.getIsDeleted() && s.getStatut() == StatutSpot.LIBRE)
                .collect(Collectors.toList());
    }

    @Override
    public void updateStatus(String id, StatutSpot newStatus) {
        spotRepository.findById(id).ifPresent(spot -> {
            spot.setStatut(newStatus);
            spot.setUpdatedAt(LocalDateTime.now());
            spotRepository.save(spot);
        });
    }
}