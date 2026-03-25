package esprit.tn.gestion_parking.entity;

import lombok.*;
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
    private LocalDateTime datetimeEntree;
    private LocalDateTime datetimeSortie;

    private String qrCode;
    private Boolean isDeleted = false;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
