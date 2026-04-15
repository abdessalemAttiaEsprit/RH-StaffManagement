package esprit.tn.gestion_parking.service;

import esprit.tn.gestion_parking.dto.RecetteDTO;
import esprit.tn.gestion_parking.entity.Recette;
import esprit.tn.gestion_parking.repository.RecetteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecetteServiceImpl implements IRecetteService {
    private final RecetteRepository recetteRepository;

    @Override
    public void enregistrerSortie(Double montant) {
        LocalDate today = LocalDate.now();
        Recette recette = recetteRepository.findByDateRecette(today)
                .orElse(Recette.builder()
                        .dateRecette(today)
                        .montantTotal(0.0)
                        .nbVehiculesSortis(0L)
                        .build());

        recette.setMontantTotal(recette.getMontantTotal() + montant);
        recette.setNbVehiculesSortis(recette.getNbVehiculesSortis() + 1);
        recetteRepository.save(recette);
    }

    @Override
    public RecetteDTO getStatistiquesGlobales() {
        LocalDate now = LocalDate.now();
        Map<String, Double> summary = new HashMap<>();

        summary.put("today", calculateSum(now, now));
        summary.put("week", calculateSum(now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), now));
        summary.put("month", calculateSum(now.withDayOfMonth(1), now));
        summary.put("year", calculateSum(now.withDayOfYear(1), now));

        List<RecetteDTO.HistoryDetail> history = recetteRepository.findAllByOrderByDateRecetteDesc().stream()
                .map(r -> new RecetteDTO.HistoryDetail(
                        r.getDateRecette().toString(),
                        r.getNbVehiculesSortis(),
                        r.getMontantTotal()))
                .collect(Collectors.toList());

        return RecetteDTO.builder().summary(summary).history(history).build();
    }

    private Double calculateSum(LocalDate start, LocalDate end) {
        return recetteRepository.findByDateRecetteBetween(start, end).stream()
                .mapToDouble(Recette::getMontantTotal).sum();
    }
}