package com.smartpark.backend.mapper;

import org.springframework.stereotype.Component;
import com.smartpark.backend.dto.ApplicationDTO;
import com.smartpark.backend.model.Application;

@Component
public class ApplicationMapper {
    
    public ApplicationDTO toDTO(Application application) {
        if (application == null) {
            return null;
        }
        
        ApplicationDTO dto = new ApplicationDTO();
        dto.setId(application.getId());
        dto.setCandidateId(application.getCandidateId());
        dto.setJobPostingId(application.getJobPostingId());
        dto.setStatus(application.getStatus());
        dto.setCoverLetter(application.getCoverLetter());
        dto.setScore(application.getScore());
        dto.setFeedback(application.getFeedback());
        dto.setAiScore(application.getAiScore());
        dto.setAiFeedback(application.getAiFeedback());
        dto.setEvaluatedAt(application.getEvaluatedAt());
        dto.setAppliedDate(application.getAppliedDate());
        
        return dto;
    }
    
    public Application toEntity(ApplicationDTO dto) {
        if (dto == null) {
            return null;
        }
        
        Application application = new Application();
        application.setId(dto.getId());
        application.setCandidateId(dto.getCandidateId());
        application.setJobPostingId(dto.getJobPostingId());
        application.setStatus(dto.getStatus());
        application.setCoverLetter(dto.getCoverLetter());
        application.setScore(dto.getScore());
        application.setFeedback(dto.getFeedback());
        application.setAiScore(dto.getAiScore());
        application.setAiFeedback(dto.getAiFeedback());
        application.setEvaluatedAt(dto.getEvaluatedAt());
        application.setAppliedDate(dto.getAppliedDate());
        
        return application;
    }
}

