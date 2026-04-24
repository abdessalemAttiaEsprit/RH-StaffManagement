package com.smartpark.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public class JobPostingDTO {
    
    private String id;
    private String title;
    private String description;
    private String department;
    private List<String> requiredSkills;
    private Double salaryMin;
    private Double salaryMax;
    private String jobType;
    private LocalDateTime datePosted;
    private LocalDateTime deadline;
    private String status;
    private Integer numberOfPositions;
    private String createdByUserId;
    private Integer applicationsCount;
    public JobPostingDTO() {
    }
    public JobPostingDTO(String title, String description, String department, List<String> requiredSkills,
                         Double salaryMin, Double salaryMax, String jobType, LocalDateTime deadline,
                         String status, Integer numberOfPositions) {
        this.title = title;
        this.description = description;
        this.department = department;
        this.requiredSkills = requiredSkills;
        this.salaryMin = salaryMin;
        this.salaryMax = salaryMax;
        this.jobType = jobType;
        this.deadline = deadline;
        this.status = status;
        this.numberOfPositions = numberOfPositions;
    }
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

