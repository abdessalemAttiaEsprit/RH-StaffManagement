package esprit.tn.gestion_parking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.HashMap;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemiseDTO {
    private String id;
    private int seuilHeures;
    private double pourcentageRemise;
    private String description;
    private String themeVisuel;
    
    // Kept flat for service logic
    private String parkingId;

    // Mimic the nested "parking" object structure expected by the frontend
    @JsonProperty("parking")
    public Map<String, String> getParking() {
        if (this.parkingId != null) {
            Map<String, String> p = new HashMap<>();
            p.put("id", this.parkingId);
            return p;
        }
        return null;
    }

    @JsonProperty("parking")
    public void setParking(Map<String, String> parking) {
        if (parking != null && parking.containsKey("id")) {
            this.parkingId = parking.get("id");
        }
    }
}