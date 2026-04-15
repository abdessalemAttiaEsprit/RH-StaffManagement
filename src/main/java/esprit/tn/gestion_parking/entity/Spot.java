package esprit.tn.gestion_parking.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "spots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Spot {
    @Id
    private String id;
    private String nom;
    private String description;
    private StatutSpot statut;
    private Double x;
    private Double y;
    private Boolean isDeleted = false;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // Many-to-One: Many spots belong to one parking
    @JsonIgnoreProperties("spots")

    private Parking parking;
    @DocumentReference(lazy = true)
    private List<Reservation>reservationList ;
}
