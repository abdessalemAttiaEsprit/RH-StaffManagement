package com.smartpark.backend.service;

import com.smartpark.backend.dto.ApplicationDTO;

import java.time.LocalDateTime;
import java.util.List;

public interface IApplicationService {
    
    ApplicationDTO applyForJob(ApplicationDTO applicationDTO);
    
    List<ApplicationDTO> getAllApplications();
    
    ApplicationDTO getApplicationById(String id);
    
    List<ApplicationDTO> getApplicationsByJobPosting(String jobPostingId);
    
    List<ApplicationDTO> getApplicationsByCandidate(String candidateId);
    
    List<ApplicationDTO> getApplicationsByStatus(String status);
    
    ApplicationDTO updateApplicationStatus(String id, String status);
    
    ApplicationDTO scoreApplication(String id, Double score, String feedback);

    ApplicationDTO scoreApplicationWithAI(String id);
    
    void rejectApplication(String id, String reason);
    
    void deleteApplication(String id);
    
    ApplicationDTO scheduleInterview(String id, LocalDateTime interviewDate, String interviewLocation);
    
    ApplicationDTO rescheduleInterview(String id, LocalDateTime newInterviewDate, String newInterviewLocation);
    
    ApplicationDTO cancelInterview(String id);
    
    List<ApplicationDTO> getScheduledInterviews();
    
    List<ApplicationDTO> getUpcomingInterviews(Integer days);
}

