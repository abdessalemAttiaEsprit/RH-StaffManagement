package esprit.tn.gestion_parking.service;

import esprit.tn.gestion_parking.dto.SpotDTO;
import esprit.tn.gestion_parking.entity.Parking;
import esprit.tn.gestion_parking.entity.Spot;
import esprit.tn.gestion_parking.entity.StatutSpot;
import esprit.tn.gestion_parking.repository.ParkingRepository;
import esprit.tn.gestion_parking.repository.SpotRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SpotServiceImpl implements ISpotService {

    @Autowired private SpotRepository spotRepository;
    @Autowired private ParkingRepository parkingRepository;

    // --- MAPPERS ---
    public SpotDTO mapToDTO(Spot spot) {
        if (spot == null) return null;
        return SpotDTO.builder()
                .id(spot.getId()).nom(spot.getNom()).description(spot.getDescription())
                .statut(spot.getStatut()).x(spot.getX()).y(spot.getY())
                .parkingId(spot.getParking() != null ? spot.getParking().getId() : null)
                .build();
    }

    public Spot mapToEntity(SpotDTO dto) {
        Spot spot = new Spot();
        spot.setNom(dto.getNom());
        spot.setDescription(dto.getDescription());
        spot.setStatut(dto.getStatut() != null ? dto.getStatut() : StatutSpot.LIBRE);
        spot.setX(dto.getX());
        spot.setY(dto.getY());
        return spot;
    }

    // --- LOGIQUE MÉTIER ---
    @Override
    public SpotDTO addSpot(SpotDTO spotDTO) {
        Spot spot = mapToEntity(spotDTO);
        spot.setCreatedAt(LocalDateTime.now());
        spot.setUpdatedAt(LocalDateTime.now());
        spot.setIsDeleted(false);

        if (spotDTO.getParkingId() != null) {
            Parking parking = parkingRepository.findById(spotDTO.getParkingId())
                    .orElseThrow(() -> new RuntimeException("Parking introuvable"));
            spot.setParking(parking);
            Spot savedSpot = spotRepository.save(spot);

            if (parking.getSpots() == null) parking.setSpots(new ArrayList<>());
            parking.getSpots().add(savedSpot);
            parkingRepository.save(parking);

            return mapToDTO(savedSpot);
        }
        return mapToDTO(spotRepository.save(spot));
    }

    @Override
    public SpotDTO updateSpot(String id, SpotDTO details) {
        return spotRepository.findById(id).map(spot -> {
            spot.setNom(details.getNom());
            spot.setDescription(details.getDescription());
            spot.setStatut(details.getStatut());
            spot.setX(details.getX());
            spot.setY(details.getY());
            spot.setUpdatedAt(LocalDateTime.now());
            return mapToDTO(spotRepository.save(spot));
        }).orElse(null);
    }

    @Override
    public List<SpotDTO> getAllSpots() {
        return spotRepository.findAll().stream()
                .filter(s -> !Boolean.TRUE.equals(s.getIsDeleted()))
                .map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public SpotDTO getById(String id) {
        return spotRepository.findById(id)
                .filter(s -> !Boolean.TRUE.equals(s.getIsDeleted()))
                .map(this::mapToDTO).orElse(null);
    }

    @Override
    public void deleteSpot(String id) {
        spotRepository.findById(id).ifPresent(spot -> {
            if (spot.getParking() != null) {
                parkingRepository.findById(spot.getParking().getId()).ifPresent(parking -> {
                    if (parking.getSpots() != null) {
                        parking.getSpots().removeIf(s -> s.getId().equals(id));
                        parkingRepository.save(parking);
                    }
                });
            }
            spotRepository.deleteById(id);
        });
    }

    @Override
    public List<SpotDTO> findByParking(String parkingId) {
        return spotRepository.findByParking_Id(parkingId).stream()
                .filter(s -> !Boolean.TRUE.equals(s.getIsDeleted()))
                .map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public List<SpotDTO> findAvailableByParking(String parkingId) {
        return spotRepository.findByParking_Id(parkingId).stream()
                .filter(s -> !Boolean.TRUE.equals(s.getIsDeleted()) && s.getStatut() == StatutSpot.LIBRE)
                .map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public void updateStatus(String id, StatutSpot newStatus) {
        spotRepository.findById(id).ifPresent(spot -> {
            spot.setStatut(newStatus);
            spot.setUpdatedAt(LocalDateTime.now());
            spotRepository.save(spot);
        });
    }

    @Override
    public List<SpotDTO> addMultipleSpots(List<SpotDTO> spots) {
        return spots.stream()
                .map(this::addSpot)
                .collect(Collectors.toList());
    }

    @Override
    public List<SpotDTO> scanAndGenerateSpots(String parkingId, MultipartFile file) throws IOException {
        // 1. Création du dossier scripts s'il n'existe pas
        Path scriptsPath = Paths.get("scripts");
        if (!Files.exists(scriptsPath)) Files.createDirectories(scriptsPath);

        // 2. Sauvegarde de l'image
        String originalName = file.getOriginalFilename();
        String extension = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf(".")) : ".jpg";
        String fileName = UUID.randomUUID().toString() + extension;
        Path tempFile = scriptsPath.resolve(fileName);

        Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);

        List<SpotDTO> savedSpots = new ArrayList<>();

        try {
            // 3. Exécution du script Python avec CHEMIN ABSOLU
            String pythonPath = "C:\\Users\\Mega-PC\\AppData\\Local\\Programs\\Python\\Python312\\python.exe";
            String scriptPath = tempFile.getParent().toAbsolutePath().resolve("detect_spots.py").toString();
            String imagePath = tempFile.toAbsolutePath().toString();

            ProcessBuilder pb = new ProcessBuilder(pythonPath, scriptPath, imagePath);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            // 4. Lecture de la réponse
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }

            String finalJson = response.toString().trim();
            System.out.println("--- DEBUG IA START ---");
            System.out.println("Contenu reçu : " + finalJson);
            System.out.println("--- DEBUG IA END ---");

            // 5. Parsing JSON
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> detectedPoints = mapper.readValue(finalJson, new TypeReference<>() {});

            // 6. Conversion en entités et sauvegarde
            for (Map<String, Object> pt : detectedPoints) {
                SpotDTO spot = SpotDTO.builder()
                        .nom((String) pt.get("nom"))
                        .description("Détecté par IA OpenCV")
                        .statut(StatutSpot.LIBRE)
                        .x(Double.valueOf(pt.get("x").toString()))
                        .y(Double.valueOf(pt.get("y").toString()))
                        .parkingId(parkingId)
                        .build();

                savedSpots.add(this.addSpot(spot));
            }

        } catch (Exception e) {
            System.err.println("ERREUR SCAN IA : " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Nettoyage
            Files.deleteIfExists(tempFile);
        }

        return savedSpots;
    }
}