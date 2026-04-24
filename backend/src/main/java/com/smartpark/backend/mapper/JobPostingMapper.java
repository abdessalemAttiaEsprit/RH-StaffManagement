package com.smartpark.backend.mapper;

import org.springframework.stereotype.Component;
import com.smartpark.backend.dto.JobPostingDTO;
import com.smartpark.backend.model.JobPosting;

@Component
public class JobPostingMapper {
    
    public JobPostingDTO toDTO(JobPosting jobPosting) {
        if (jobPosting == null) {
            return null;
        }
        
        JobPostingDTO dto = new JobPostingDTO();
        dto.setId(jobPosting.getId());
        dto.setTitle(jobPosting.getTitle());
        dto.setDescription(jobPosting.getDescription());
        dto.setDepartment(jobPosting.getDepartment());
        dto.setRequiredSkills(jobPosting.getRequiredSkills());
        dto.setSalaryMin(jobPosting.getSalaryMin());
        dto.setSalaryMax(jobPosting.getSalaryMax());
        dto.setJobType(jobPosting.getJobType());
        dto.setDatePosted(jobPosting.getDatePosted());
        dto.setDeadline(jobPosting.getDeadline());
        dto.setStatus(jobPosting.getStatus());
        dto.setNumberOfPositions(jobPosting.getNumberOfPositions());
        dto.setCreatedByUserId(jobPosting.getCreatedByUserId());
        dto.setApplicationsCount(jobPosting.getApplicationsCount());
        
        return dto;
    }
    
    public JobPosting toEntity(JobPostingDTO dto) {
        if (dto == null) {
            return null;
        }
        
        JobPosting jobPosting = new JobPosting();
        jobPosting.setId(dto.getId());
        jobPosting.setTitle(dto.getTitle());
        jobPosting.setDescription(dto.getDescription());
        jobPosting.setDepartment(dto.getDepartment());
        jobPosting.setRequiredSkills(dto.getRequiredSkills());
        jobPosting.setSalaryMin(dto.getSalaryMin());
        jobPosting.setSalaryMax(dto.getSalaryMax());
        jobPosting.setJobType(dto.getJobType());
        jobPosting.setDatePosted(dto.getDatePosted());
        jobPosting.setDeadline(dto.getDeadline());
        jobPosting.setStatus(dto.getStatus());
        jobPosting.setNumberOfPositions(dto.getNumberOfPositions());
        jobPosting.setCreatedByUserId(dto.getCreatedByUserId());
        jobPosting.setApplicationsCount(dto.getApplicationsCount());
        
        return jobPosting;
    }
}

