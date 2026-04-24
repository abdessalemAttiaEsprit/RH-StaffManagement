package com.smartpark.backend.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import com.smartpark.backend.model.Candidate;

import java.util.List;
import java.util.Optional;

public interface ICandidateRepository extends MongoRepository<Candidate, String> {
    
    Optional<Candidate> findByEmail(String email);
    
    Optional<Candidate> findByCin(String cin);
    
    @Query("{ 'skills' : { $in: ?0 } }")
    List<Candidate> findBySkills(List<String> skills);
    
    @Query("{ '$text': { '$search': ?0 } }")
    List<Candidate> searchByName(String searchTerm);
}

