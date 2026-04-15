package esprit.tn.gestion_parking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationDTO {
    private String id;
    private String matricule;
    private LocalDateTime datetimeEntree;
    private LocalDateTime datetimeSortie;
    private double montant;
    private Double montantFinal;
    private String statusAction;
    private String qrCode;

    private String spotId;
    private String spotNom;

    private String parkingId;
    private String parkingNom; // ✅ AJOUTE CETTE LIGNE

    private double tarifDepassement;
    private double remiseRetard;

    private String voitureMarque;   // ex: Volkswagen
    private String voitureCouleur;  // ex: Blanc
    private String voitureModele;   // ex: Golf 7
    private boolean spontane;     // true si pas de réservation préalable
    private double scoreConfiance;  // Pour savoir si l'IA est sûre d'elle (0.0 à 1.0)
}