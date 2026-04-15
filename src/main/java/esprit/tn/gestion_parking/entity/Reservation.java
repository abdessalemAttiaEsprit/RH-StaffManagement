package esprit.tn.gestion_parking.entity;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

import java.time.LocalDateTime;

@Data
@Document(collection = "reservation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Reservation {
    private String id;
    private String matricule;
    @DocumentReference

    private Spot spot;
    private double montant;
    private LocalDateTime date;
    private LocalDateTime dateSortie;
    private LocalDateTime datetimeEntree;
    private LocalDateTime datetimeSortie;
    private String statusAction = "ATTENTE";
    private Double montantFinal;
    private String qrCode;
    private Boolean isDeleted = false;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @DBRef
    private Parking parking;
    // Dans Reservation.java
    private Double tarifDepassement; // Utilise Double au lieu de double
    private Double remiseRetard;
    private String voitureMarque;
    private String voitureCouleur;
    private String voitureModele;
    private boolean spontane;
    private Double scoreConfiance;// Utilise Double au lieu de double
}
