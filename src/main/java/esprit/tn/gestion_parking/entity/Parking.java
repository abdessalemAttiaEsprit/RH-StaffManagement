package esprit.tn.gestion_parking.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "parking")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Parking {
    @Id
    private String id;
    private String nom;
    private String description;
    private TypeParking typeParking; // Utilisation de l'Enum
    private Boolean isDeleted = false;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private double prixInitial;
    private double prixPromos;
    private LocalDate dateDebutPromos;
    private LocalDate dateFinPromos;
    private boolean isEvent;
    @DocumentReference(lazy = true)
    @JsonIgnoreProperties("parking")
    private List<Spot> spots;

}
