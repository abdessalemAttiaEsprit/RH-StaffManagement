package esprit.tn.gestion_parking.repository;

import esprit.tn.gestion_parking.entity.Recette;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RecetteRepository extends MongoRepository<Recette, String> {
    Optional<Recette> findByDateRecette(LocalDate date);
    List<Recette> findByDateRecetteBetween(LocalDate start, LocalDate end);
    List<Recette> findAllByOrderByDateRecetteDesc();
}