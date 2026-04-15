package esprit.tn.gestion_parking;

import esprit.tn.gestion_parking.entity.Recette;
import esprit.tn.gestion_parking.repository.RecetteRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDate;

@SpringBootApplication
public class GestionParkingApplication {

    public static void main(String[] args) {
        SpringApplication.run(GestionParkingApplication.class, args);
    }

    /**
     * Ce bloc s'exécute au démarrage de l'application.
     * Il remplit la base MongoDB avec des données de test si elle est vide.
     */
    @Bean
    CommandLineRunner initRecettes(RecetteRepository repo) {
        return args -> {
            // On vérifie si la collection 'recettes' est vide
            if (repo.count() == 0) {
                LocalDate today = LocalDate.now();

                // On crée 3 jours de recettes pour tester les filtres (Jour/Mois/An)
                repo.save(Recette.builder()
                        .dateRecette(today)
                        .montantTotal(145.50)
                        .nbVehiculesSortis(12L)
                        .build());

                repo.save(Recette.builder()
                        .dateRecette(today.minusDays(1))
                        .montantTotal(98.00)
                        .nbVehiculesSortis(8L)
                        .build());

                repo.save(Recette.builder()
                        .dateRecette(today.minusDays(2))
                        .montantTotal(215.00)
                        .nbVehiculesSortis(15L)
                        .build());

                System.out.println("✅ Succès : Données de test insérées dans MongoDB !");
            }
        };
    }
}