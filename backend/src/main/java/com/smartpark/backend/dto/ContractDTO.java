package com.smartpark.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ContractDTO {
    private String role;
    private String typeContrat;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private Double salaireBase;
    private Double tauxHoraireSup;
    private Map<String, Double> avantages;
}
