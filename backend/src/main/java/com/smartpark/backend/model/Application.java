package com.smartpark.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

@Document(collection = "applications")
public class Application {
    
    @Id
    private String id;
    
    @Field("candidateId")
    private String candidateId;
    
    @Field("jobPostingId")
    private String jobPostingId;
    
    @Field("status")
    private String status; // SUBMITTED, UNDER_REVIEW, SHORTLISTED, REJECTED, HIRED
    
    @Field("coverLetter")
    private String coverLetter;
    
    @Field("score")
    private Double score;
    
    @Field("feedback")
    private String feedback;

    @Field("aiScore")
    private Double aiScore;

    @Field("aiFeedback")
    private String aiFeedback;

    @Field("evaluatedAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime evaluatedAt;
    
    @Field("appliedDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime appliedDate;

    @Field("interviewDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime interviewDate;

    @Field("interviewLocation")
    private String interviewLocation;

    // Constructors
    public Application() {
    }

    public Application(String candidateId, String jobPostingId, String status, String coverLetter) {
        this.candidateId = candidateId;
        this.jobPostingId = jobPostingId;
        this.status = status;
        this.coverLetter = coverLetter;
        this.appliedDate = LocalDateTime.now();
        this.score = 0.0;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCoverLetter() {
        return coverLetter;
    }

    public void setCoverLetter(String coverLetter) {
        this.coverLetter = coverLetter;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public Double getAiScore() {
        return aiScore;
    }

    public void setAiScore(Double aiScore) {
        this.aiScore = aiScore;
    }

    public String getAiFeedback() {
        return aiFeedback;
    }

    public void setAiFeedback(String aiFeedback) {
        this.aiFeedback = aiFeedback;
    }

    public LocalDateTime getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(LocalDateTime evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }

    public LocalDateTime getAppliedDate() {
        return appliedDate;
    }

    public void setAppliedDate(LocalDateTime appliedDate) {
        this.appliedDate = appliedDate;
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
}

