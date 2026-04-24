package com.smartpark.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PersonnelDTO {
    private String id; // Gardé pour les mises à jour (PUT)
    private String nom;
    private String prenom;
    private String telephone;
    private String email;
    private String cin;

    private String matricule;
    private String cnssNumber;
    private String rib;
    private List<AbsenceDTO> absences = new ArrayList<>();    // Absences et Contrat
    private ContractDTO contrat; // Le contrat est imbriqué ici

    private Long absenceQuotaMonthlyDays;
    private Long absenceQuotaEarnedDays;
    private Long absenceQuotaUsedJustifiedDays;
    private Long absenceQuotaRemainingDays;
}
