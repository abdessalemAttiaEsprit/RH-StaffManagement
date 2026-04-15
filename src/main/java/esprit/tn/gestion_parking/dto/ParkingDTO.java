package esprit.tn.gestion_parking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParkingDTO {
    private String id;
    private String nom;
    private String description;
    private String typeParking;
    private double prixInitial;
    private double prixPromos;
    private double tarifDepassement;
    private double remiseRetard;
    private boolean isEvent;
    private LocalDate dateDebutPromos;
    private LocalDate dateFinPromos;
    private List<SpotDTO> spots;
    private List<RemiseDTO> remises;

}