package esprit.tn.gestion_parking.service;

import esprit.tn.gestion_parking.dto.ReservationDTO;
import esprit.tn.gestion_parking.entity.Reservation;
import esprit.tn.gestion_parking.entity.Spot;
import esprit.tn.gestion_parking.repository.ReservationRepository;
import esprit.tn.gestion_parking.repository.SpotRepository;
import esprit.tn.gestion_parking.repository.ParkingRepository;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReservationServiceImpl implements IReservationService {

    @Autowired private ReservationRepository reservationRepository;
    @Autowired private SpotRepository spotRepository;
    @Autowired private ParkingRepository parkingRepository;
    @Autowired private IRecetteService recetteService;

    /**
     * ✅ MAPPER : Transforme l'entité en DTO pour le Frontend
     * C'est ici qu'on injecte les tarifs du parking dans le DTO
     */
    private ReservationDTO mapToDTO(Reservation res) {
        if (res == null) return null;

        ReservationDTO dto = ReservationDTO.builder()
                .id(res.getId())
                .matricule(res.getMatricule())
                .datetimeEntree(res.getDatetimeEntree())
                .datetimeSortie(res.getDatetimeSortie())
                .montant(res.getMontant())
                .montantFinal(res.getMontantFinal())
                .statusAction(res.getStatusAction())
                .qrCode(res.getQrCode())
                .voitureMarque(res.getVoitureMarque())
                .voitureCouleur(res.getVoitureCouleur())
                .voitureModele(res.getVoitureModele())
                .spontane(res.isSpontane())
                .scoreConfiance(res.getScoreConfiance() != null ? res.getScoreConfiance() : 0.0)
                .build();

        if (res.getSpot() != null) {
            // On récupère les infos du Spot
            dto.setSpotId(res.getSpot().getId());
            dto.setSpotNom(res.getSpot().getNom());

            if (res.getSpot().getParking() != null) {
                // ✅ RÉCUPÉRATION DES TARIFS DEPUIS L'OBJET PARKING
                // ✅ RÉCUPÉRATION DES TARIFS DIRECTEMENT DEPUIS LA DB (Pour contourner le cache document de MongoDB)
                String pId = res.getSpot().getParking().getId();
                dto.setParkingId(pId);
                dto.setParkingNom(res.getSpot().getParking().getNom());

                parkingRepository.findById(pId).ifPresent(p -> {
                    double td = (p.getTarifDepassement() != null) ? p.getTarifDepassement() : 0.0;
                    double rr = (p.getRemiseRetard() != null) ? p.getRemiseRetard() : 0.0;
                    
                    dto.setTarifDepassement(td);
                    dto.setRemiseRetard(rr);
                });
            }
        } else {
            dto.setSpotNom("Sans place");
        }

        return dto;
    }

    private Reservation mapToEntity(ReservationDTO dto) {
        Reservation res = new Reservation();
        res.setId(dto.getId());
        res.setMatricule(dto.getMatricule());
        res.setDatetimeEntree(dto.getDatetimeEntree());
        res.setDatetimeSortie(dto.getDatetimeSortie());
        res.setMontant(dto.getMontant());
        res.setMontantFinal(dto.getMontantFinal());
        res.setStatusAction(dto.getStatusAction());
        return res;
    }

    @Override
    public ReservationDTO createReservation(ReservationDTO dto) {
        if (dto.getSpotId() == null) throw new RuntimeException("Spot ID is required");

        // Récupère le spot (avec son parking attaché)
        Spot spot = spotRepository.findById(dto.getSpotId())
                .orElseThrow(() -> new RuntimeException("Spot introuvable"));

        if (!isSpotAvailable(dto.getSpotId(), dto.getDatetimeEntree(), dto.getDatetimeSortie())) {
            throw new RuntimeException("Conflict: Ce spot est déjà réservé.");
        }

        Reservation res = mapToEntity(dto);
        res.setSpot(spot);
        res.setCreatedAt(LocalDateTime.now());
        res.setUpdatedAt(LocalDateTime.now());
        res.setDate(LocalDateTime.now());
        res.setIsDeleted(false);
        res.setQrCode(UUID.randomUUID().toString());

        // Calcul du prix de base (ex: 5 TND/H)
        if (res.getMontant() <= 0) {
            res.setMontant(calculatePrice(res.getDatetimeEntree(), res.getDatetimeSortie()));
        }

        return mapToDTO(reservationRepository.save(res));
    }

    @Override
    public ReservationDTO getById(String id) {
        // FindById recharge l'entité proprement depuis MongoDB
        return reservationRepository.findById(id)
                .filter(r -> !r.getIsDeleted())
                .map(this::mapToDTO)
                .orElse(null);
    }

    @Override
    public ReservationDTO updateReservation(String id, ReservationDTO updatedDto) {
        Reservation existing = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Réservation introuvable"));

        String oldStatus = existing.getStatusAction();

        // Mise à jour des champs
        existing.setMatricule(updatedDto.getMatricule());
        existing.setDatetimeEntree(updatedDto.getDatetimeEntree());
        existing.setDatetimeSortie(updatedDto.getDatetimeSortie());

        if (updatedDto.getStatusAction() != null) {
            existing.setStatusAction(updatedDto.getStatusAction());
        }

        if (updatedDto.getMontantFinal() != null) {
            existing.setMontantFinal(updatedDto.getMontantFinal());
        }

        existing.setUpdatedAt(LocalDateTime.now());
        Reservation saved = reservationRepository.save(existing);

        // Logique de recette lors de la sortie
        if (!"SORTIE_VALIDEE".equals(oldStatus) && "SORTIE_VALIDEE".equals(updatedDto.getStatusAction())) {
            double total = (saved.getMontantFinal() != null) ? saved.getMontantFinal() : saved.getMontant();
            recetteService.enregistrerSortie(total);
        }

        return mapToDTO(saved);
    }

    @Override
    public List<ReservationDTO> getAll() {
        return reservationRepository.findByIsDeletedFalse().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public double calculatePrice(LocalDateTime start, LocalDateTime end) {
        long hours = Duration.between(start, end).toHours();
        if (hours <= 0) hours = 1;
        return hours * 5.0; // Prix fixe par défaut
    }

    @Override
    public boolean isSpotAvailable(String spotId, LocalDateTime start, LocalDateTime end) {
        List<Reservation> existing = reservationRepository.findBySpotId(spotId);
        return existing.stream().filter(r -> !r.getIsDeleted())
                .noneMatch(r -> start.isBefore(r.getDatetimeSortie()) && end.isAfter(r.getDatetimeEntree()));
    }

    @Override
    public void cancelReservation(String id) {
        reservationRepository.findById(id).ifPresent(res -> {
            res.setIsDeleted(true);
            res.setUpdatedAt(LocalDateTime.now());
            reservationRepository.save(res);
        });
    }

    @Override
    public List<ReservationDTO> findBySpot(String spotId) {
        return reservationRepository.findBySpotIdAndIsDeletedFalse(spotId).stream()
                .map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public List<ReservationDTO> findByVehicle(String matricule) {
        return reservationRepository.findByMatriculeIgnoreCase(matricule).stream()
                .filter(r -> !r.getIsDeleted())
                .map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public List<ReservationDTO> findByParking(String parkingId) {
        List<Spot> spots = spotRepository.findByParking_Id(parkingId);
        if (spots.isEmpty()) return List.of();

        List<ObjectId> spotObjectIds = spots.stream()
                .map(s -> new ObjectId(s.getId()))
                .collect(Collectors.toList());

        return reservationRepository.findBySpotIdsIn(spotObjectIds).stream()
                .map(this::mapToDTO).collect(Collectors.toList());
    }


    public ReservationDTO enregistrerPassageAuto(ReservationDTO iaDto) {
        // 1. Chercher s'il y a une réservation future pour cette matricule
        List<Reservation> futures = reservationRepository.findByMatriculeIgnoreCase(iaDto.getMatricule())
                .stream()
                .filter(r -> !r.getIsDeleted() && "EN_ATTENTE".equals(r.getStatusAction()))
                .collect(Collectors.toList());

        Reservation session;

        if (!futures.isEmpty()) {
            // Cas A : Le client a réservé
            session = futures.get(0);
            session.setStatusAction("EN_COURS"); // On active la session
            session.setVoitureMarque(iaDto.getVoitureMarque()); // On vérifie la marque pour la sortie
            session.setVoitureCouleur(iaDto.getVoitureCouleur());
            session.setSpontane(false);
        } else {
            // Cas B : Entrée spontanée (Pas de réservation)
            session = new Reservation();
            session.setMatricule(iaDto.getMatricule());
            session.setDatetimeEntree(LocalDateTime.now());
            session.setVoitureMarque(iaDto.getVoitureMarque());
            session.setVoitureCouleur(iaDto.getVoitureCouleur());
            session.setSpontane(true);
            session.setStatusAction("EN_COURS");
            session.setMontant(0.0); // Le prix sera calculé à la sortie
        }

        return mapToDTO(reservationRepository.save(session));
    }
}