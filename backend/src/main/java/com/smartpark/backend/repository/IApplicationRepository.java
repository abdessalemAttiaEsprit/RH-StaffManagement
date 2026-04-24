package com.smartpark.backend.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import com.smartpark.backend.model.Application;

import java.util.List;

public interface IApplicationRepository extends MongoRepository<Application, String> {
    
    List<Application> findByJobPostingId(String jobPostingId);
    
    List<Application> findByCandidateId(String candidateId);
    
    List<Application> findByStatus(String status);
    
    List<Application> findByJobPostingIdAndStatus(String jobPostingId, String status);
    
    /**
     * Delete all applications for a candidate (cascade delete)
     */
    Long deleteByCandidateId(String candidateId);
}

