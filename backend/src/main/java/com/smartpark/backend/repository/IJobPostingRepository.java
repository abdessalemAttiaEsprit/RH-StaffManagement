package com.smartpark.backend.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import com.smartpark.backend.model.JobPosting;

import java.time.LocalDateTime;
import java.util.List;

public interface IJobPostingRepository extends MongoRepository<JobPosting, String> {
    
    List<JobPosting> findByStatus(String status);
    
    List<JobPosting> findByDepartment(String department);
    
    List<JobPosting> findByStatusAndDepartment(String status, String department);
    
    @Query("{ 'deadline' : { $gte: ?0 } }")
    List<JobPosting> findActivePostings(LocalDateTime date);
    
    @Query("{ 'requiredSkills' : { $in: ?0 } }")
    List<JobPosting> findBySkills(List<String> skills);
}

