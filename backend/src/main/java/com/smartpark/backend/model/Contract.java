package com.smartpark.backend.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.Map;

@Document(collection = "contracts")
@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Setter
public class Contract {
    @Id
    private String id;
    // Attributs Administratif
    private String role ;
    private String typeContrat;
    private LocalDate dateDebut;
    private LocalDate dateFin;

    // Attributs Financiers
    private Double salaireBase;
    private Double tauxHoraireSup;
    private Map<String, Double> avantages;
}

