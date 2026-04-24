package com.smartpark.backend.dto;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

public class InterviewDTO {
    private String id;
    private String applicationId;
    private String candidateId;
    private String jobPostingId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime interviewDate;
    private String interviewLocation;
    private String status;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    public InterviewDTO() {
    }
    public InterviewDTO(String applicationId, String candidateId, String jobPostingId, LocalDateTime interviewDate, String interviewLocation) {
        this.applicationId = applicationId;
        this.candidateId = candidateId;
        this.jobPostingId = jobPostingId;
        this.interviewDate = interviewDate;
        this.interviewLocation = interviewLocation;
        this.status = "SCHEDULED";
    }
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getApplicationId() {
        return applicationId;
    }
    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }
    public String getCandidateId() {
        return candidateId;
    }
    public void setCandidateId(String candidateId) {
        this.candidateId = candidateId;
    }
    public String getJobPostingId() {
        return jobPostingId;
    }
    public void setJobPostingId(String jobPostingId) {
        this.jobPostingId = jobPostingId;
    }
    public LocalDateTime getInterviewDate() {
        return interviewDate;
    }
    public void setInterviewDate(LocalDateTime interviewDate) {
        this.interviewDate = interviewDate;
    }
    public String getInterviewLocation() {
        return interviewLocation;
    }
    public void setInterviewLocation(String interviewLocation) {
        this.interviewLocation = interviewLocation;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}


