package com.smartpark.backend.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import com.smartpark.backend.model.Personnel;

import java.util.Optional;

@Repository
public interface IPersonnelRepo extends MongoRepository<Personnel, String> {
    // Vous pouvez toujours chercher par matricule ou nom
    Optional<Personnel> findByMatricule(String matricule);

    Boolean existsByEmail(String email);
    Boolean existsByMatricule(String matricule);

    void deleteByMatricule(String matricule);
}


