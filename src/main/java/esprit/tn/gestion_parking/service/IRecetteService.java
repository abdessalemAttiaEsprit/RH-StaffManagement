package esprit.tn.gestion_parking.service;

import esprit.tn.gestion_parking.dto.RecetteDTO;

public interface IRecetteService {
    void enregistrerSortie(Double montant);
    RecetteDTO getStatistiquesGlobales();
}