package esprit.tn.gestion_parking.entity;

import com.fasterxml.jackson.annotation.*;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    // Dans Parking.java
    private Double tarifDepassement; // Utilisez Double (objet) pour éviter les erreurs de null
    private Double remiseRetard;
    private LocalDate dateDebutPromos;
    private LocalDate dateFinPromos;
    @JsonProperty("isEvent")
    private boolean isEvent;
    @JsonSetter("isEvent")
    public void setEvent(boolean event) {
        this.isEvent = event;
    }

    @JsonGetter("isEvent")
    public boolean isEvent() {
        return this.isEvent;
    }

    @DocumentReference(lazy = true)
    @JsonIgnoreProperties("parking")

    private List<Spot> spots;
    @JsonIgnore // 🛑 C'EST LA CLÉ MAGIQUE ICI
    @DocumentReference(lazy = true)
    private List<Remise> remises = new ArrayList<>();

}
