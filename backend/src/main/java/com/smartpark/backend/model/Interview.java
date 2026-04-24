package com.smartpark.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

@Document(collection = "interviews")
public class Interview {
    @Id
    private String id;

    @Field("applicationId")
    private String applicationId;

    @Field("candidateId")
    private String candidateId;

    @Field("jobPostingId")
    private String jobPostingId;

    @Field("interviewDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime interviewDate;

    @Field("interviewLocation")
    private String interviewLocation;

    @Field("status")
    private String status; // SCHEDULED, COMPLETED, CANCELLED

    @Field("createdAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    // Constructors
    public Interview() {
        this.createdAt = LocalDateTime.now();
        this.status = "SCHEDULED";
    }

    public Interview(String applicationId, String candidateId, String jobPostingId, LocalDateTime interviewDate, String interviewLocation) {
        this.applicationId = applicationId;
        this.candidateId = candidateId;
        this.jobPostingId = jobPostingId;
        this.interviewDate = interviewDate;
        this.interviewLocation = interviewLocation;
        this.createdAt = LocalDateTime.now();
        this.status = "SCHEDULED";
    }

    // Getters and Setters
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

