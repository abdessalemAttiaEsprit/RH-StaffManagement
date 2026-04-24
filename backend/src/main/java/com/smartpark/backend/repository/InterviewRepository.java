package com.smartpark.backend.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import com.smartpark.backend.model.Interview;
import java.util.List;

@Repository
public interface InterviewRepository extends MongoRepository<Interview, String> {
    List<Interview> findByCandidateId(String candidateId);
    List<Interview> findByApplicationId(String applicationId);
    List<Interview> findByJobPostingId(String jobPostingId);
    List<Interview> findByStatus(String status);
    

    Long deleteByCandidateId(String candidateId); // Cascade delete interviews when candidate is deleted
    Long deleteByApplicationId(String applicationId); // Cascade delete interviews when application is deleted
}

