package com.smartpark.backend.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.smartpark.backend.model.PersonnelRequest;

import java.util.List;

public interface IPersonnelRequestRepo extends MongoRepository<PersonnelRequest, String> {
    List<PersonnelRequest> findByMatriculeOrderByCreatedAtDesc(String matricule);

    List<PersonnelRequest> findAllByOrderByCreatedAtDesc();

    long deleteByMatricule(String matricule);
}

