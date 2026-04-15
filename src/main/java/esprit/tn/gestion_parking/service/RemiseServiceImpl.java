package esprit.tn.gestion_parking.service;

import esprit.tn.gestion_parking.dto.RemiseDTO;
import esprit.tn.gestion_parking.entity.Parking;
import esprit.tn.gestion_parking.entity.Remise;
import esprit.tn.gestion_parking.repository.ParkingRepository;
import esprit.tn.gestion_parking.repository.RemiseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RemiseServiceImpl implements IRemiseService {

    @Autowired private RemiseRepository remiseRepository;
    @Autowired private ParkingRepository parkingRepository;

    private RemiseDTO mapToDTO(Remise remise) {
        if (remise == null) return null;
        return RemiseDTO.builder()
                .id(remise.getId())
                .seuilHeures((int) remise.getSeuilHeures())
                .pourcentageRemise(remise.getPourcentageRemise())
                .description(remise.getDescription())
                .themeVisuel(remise.getThemeVisuel())
                .parkingId(remise.getParking() != null ? remise.getParking().getId() : null)
                .build();
    }

    private Remise mapToEntity(RemiseDTO dto) {
        Remise remise = new Remise();
        remise.setSeuilHeures(dto.getSeuilHeures());
        remise.setPourcentageRemise(dto.getPourcentageRemise());
        remise.setDescription(dto.getDescription());
        remise.setThemeVisuel(dto.getThemeVisuel());
        return remise;
    }

    @Override
    public RemiseDTO addRemise(RemiseDTO dto) {
        Remise remise = mapToEntity(dto);
        remise.setCreatedAt(LocalDateTime.now());
        remise.setUpdatedAt(LocalDateTime.now());
        remise.setIsDeleted(false);

        if (dto.getParkingId() != null) {
            Parking parking = parkingRepository.findById(dto.getParkingId())
                    .orElseThrow(() -> new RuntimeException("Parking introuvable"));
            remise.setParking(parking);
            Remise saved = remiseRepository.save(remise);

            if (parking.getRemises() == null) parking.setRemises(new ArrayList<>());
            parking.getRemises().add(saved);
            parkingRepository.save(parking);
            return mapToDTO(saved);
        }
        return mapToDTO(remiseRepository.save(remise));
    }

    @Override
    public RemiseDTO updateRemise(String id, RemiseDTO details) {
        return remiseRepository.findById(id).map(remise -> {
            remise.setSeuilHeures(details.getSeuilHeures());
            remise.setPourcentageRemise(details.getPourcentageRemise());
            remise.setDescription(details.getDescription());
            remise.setThemeVisuel(details.getThemeVisuel());
            remise.setUpdatedAt(LocalDateTime.now());
            Remise saved = remiseRepository.save(remise);
            return mapToDTO(saved);
        }).orElse(null);
    }

    @Override
    public List<RemiseDTO> getByParking(String parkingId) {
        return remiseRepository.findByParkingIdAndIsDeletedFalse(parkingId).stream()
                .map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public void deleteRemise(String id) {
        remiseRepository.findById(id).ifPresent(remise -> {
            remise.setIsDeleted(true);
            remise.setUpdatedAt(LocalDateTime.now());
            remiseRepository.save(remise);
        });
    }
}