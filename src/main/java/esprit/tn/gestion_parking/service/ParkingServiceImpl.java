package esprit.tn.gestion_parking.service;

import esprit.tn.gestion_parking.entity.Parking;
import esprit.tn.gestion_parking.entity.Spot;
import esprit.tn.gestion_parking.entity.StatutSpot;
import esprit.tn.gestion_parking.repository.ParkingRepository;
import esprit.tn.gestion_parking.repository.SpotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ParkingServiceImpl implements IParkingService {

    @Autowired
    private ParkingRepository parkingRepository;

    // 1. Inject the SpotRepository so we can save the children
    @Autowired
    private SpotRepository spotRepository;

    @Override
    public Parking addParking(Parking parking) {
        // Step 1: Initialize Parking defaults
        parking.setCreatedAt(LocalDateTime.now());
        parking.setUpdatedAt(LocalDateTime.now());
        parking.setIsDeleted(false);

        // Step 2: Extract the spots from the payload and temporarily detach them
        List<Spot> providedSpots = parking.getSpots();
        parking.setSpots(new ArrayList<>());

        // Step 3: Save the Parking FIRST so MongoDB generates its ID
        Parking savedParking = parkingRepository.save(parking);

        // Step 4: If the user provided spots, prepare and save them in the Spot collection
        if (providedSpots != null && !providedSpots.isEmpty()) {
            List<Spot> savedSpots = new ArrayList<>();

            for (Spot spot : providedSpots) {
                spot.setCreatedAt(LocalDateTime.now());
                spot.setUpdatedAt(LocalDateTime.now());
                spot.setIsDeleted(false);
                if (spot.getStatut() == null) {
                    spot.setStatut(StatutSpot.LIBRE); // Default status
                }

                // Link the child to the saved parent
                spot.setParking(savedParking);

                // Save the spot to its own collection
                savedSpots.add(spotRepository.save(spot));
            }

            // Step 5: Attach the saved spots back to the parking and update it
            savedParking.setSpots(savedSpots);
            savedParking = parkingRepository.save(savedParking);
        }

        return savedParking;
    }

    @Override
    public Parking updateParking(String id, Parking parkingDetails) {
        return parkingRepository.findById(id).map(existingParking -> {
            existingParking.setNom(parkingDetails.getNom());
            existingParking.setDescription(parkingDetails.getDescription());
            existingParking.setPrixInitial(parkingDetails.getPrixInitial());
            existingParking.setUpdatedAt(LocalDateTime.now());
            return parkingRepository.save(existingParking);
        }).orElse(null);
    }

    @Override
    public List<Parking> findAll() {
        // Best Practice: Use the custom repository method we made earlier
        // instead of loading everything and filtering in Java!
        return parkingRepository.findByIsDeletedFalse();
    }

    @Override
    public Parking findById(String id) {
        return parkingRepository.findByIdAndIsDeletedFalse(id).orElse(null);
    }

    @Override
    public void delete(String id) {
        parkingRepository.findById(id).ifPresent(parking -> {
            parking.setIsDeleted(true);
            parking.setUpdatedAt(LocalDateTime.now());
            parkingRepository.save(parking);
        });
    }

    @Override
    public List<Parking> findActivePromotions() {
        LocalDateTime now = LocalDateTime.now();
        // Here we can still use streams if we don't have a specific Mongo query for dates
        return parkingRepository.findByIsDeletedFalse().stream()
                .filter(p -> p.getPrixPromos() > 0
                        && p.getDateDebutPromos() != null && p.getDateDebutPromos().isBefore(now.toLocalDate())
                        && p.getDateFinPromos() != null && p.getDateFinPromos().isAfter(now.toLocalDate()))
                .collect(Collectors.toList());
    }
}