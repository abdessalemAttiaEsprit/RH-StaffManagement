package com.smartpark.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "jobPostings")
public class JobPosting {
    
    @Id
    private String id;
    
    @Field("title")
    private String title;
    
    @Field("description")
    private String description;
    
    @Field("department")
    private String department;
    
    @Field("requiredSkills")
    private List<String> requiredSkills;
    
    @Field("salaryMin")
    private Double salaryMin;
    
    @Field("salaryMax")
    private Double salaryMax;
    
    @Field("jobType")
    private String jobType; // FULL_TIME, CONTRACT, PART_TIME
    
    @Field("datePosted")
    private LocalDateTime datePosted;
    
    @Field("deadline")
    private LocalDateTime deadline;
    
    @Field("status")
    private String status; // OPEN, CLOSED, FILLED
    
    @Field("numberOfPositions")
    private Integer numberOfPositions;
    
    @Field("createdByUserId")
    private String createdByUserId;
    
    @Field("applicationsCount")
    private Integer applicationsCount;

    // Constructors
    public JobPosting() {
    }

    public JobPosting(String title, String description, String department, List<String> requiredSkills,
                      Double salaryMin, Double salaryMax, String jobType, LocalDateTime deadline,
                      String status, Integer numberOfPositions) {
        this.title = title;
        this.description = description;
        this.department = department;
        this.requiredSkills = requiredSkills;
        this.salaryMin = salaryMin;
        this.salaryMax = salaryMax;
        this.jobType = jobType;
        this.datePosted = LocalDateTime.now();
        this.deadline = deadline;
        this.status = status;
        this.numberOfPositions = numberOfPositions;
        this.applicationsCount = 0;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public List<String> getRequiredSkills() {
        return requiredSkills;
    }

    public void setRequiredSkills(List<String> requiredSkills) {
        this.requiredSkills = requiredSkills;
    }

    public Double getSalaryMin() {
        return salaryMin;
    }

    public void setSalaryMin(Double salaryMin) {
        this.salaryMin = salaryMin;
    }

    public Double getSalaryMax() {
        return salaryMax;
    }

    public void setSalaryMax(Double salaryMax) {
        this.salaryMax = salaryMax;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public LocalDateTime getDatePosted() {
        return datePosted;
    }

    public void setDatePosted(LocalDateTime datePosted) {
        this.datePosted = datePosted;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getNumberOfPositions() {
        return numberOfPositions;
    }

    public void setNumberOfPositions(Integer numberOfPositions) {
        this.numberOfPositions = numberOfPositions;
    }

    public String getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(String createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public Integer getApplicationsCount() {
        return applicationsCount;
    }

    public void setApplicationsCount(Integer applicationsCount) {
        this.applicationsCount = applicationsCount;
    }
}

