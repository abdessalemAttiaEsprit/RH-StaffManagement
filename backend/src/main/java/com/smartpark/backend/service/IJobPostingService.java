package com.smartpark.backend.service;

import com.smartpark.backend.dto.JobPostingDTO;
import com.smartpark.backend.model.JobPosting;

import java.util.List;

public interface IJobPostingService {
    
    JobPostingDTO createJobPosting(JobPostingDTO jobPostingDTO);
    
    List<JobPostingDTO> getAllJobPostings();
    
    List<JobPostingDTO> getOpenJobPostings();
    
    JobPostingDTO getJobPostingById(String id);
    
    JobPostingDTO updateJobPosting(String id, JobPostingDTO jobPostingDTO);
    
    void closeJobPosting(String id);
    
    void deleteJobPosting(String id);
    
    List<JobPostingDTO> findByDepartment(String department);
    
    List<JobPostingDTO> findByStatus(String status);
    
    List<JobPostingDTO> findBySkills(List<String> skills);
}

