package esprit.tn.gestion_parking.service;

import esprit.tn.gestion_parking.dto.RemiseDTO;
import java.util.List;

public interface IRemiseService {
    RemiseDTO addRemise(RemiseDTO remiseDTO);
    RemiseDTO updateRemise(String id, RemiseDTO remiseDTO);
    List<RemiseDTO> getByParking(String parkingId);
    void deleteRemise(String id);
}