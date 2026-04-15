package esprit.tn.gestion_parking.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import java.time.LocalDate;

@Document(collection = "recettes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Recette {
    @Id
    private String id;

    @Indexed(unique = true)
    private LocalDate dateRecette; // Une seule entrée par jour pour cumuler les gains

    private Double montantTotal;
    private Long nbVehiculesSortis;
}