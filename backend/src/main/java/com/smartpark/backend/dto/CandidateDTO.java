package com.smartpark.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class CandidateDTO {
    
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String cin;
    private LocalDate dateOfBirth;
    private String currentTitle;
    private String currentCompany;
    private List<String> skills;
    private Integer yearsOfExperience;
    private String cvFileId;
    private LocalDateTime registrationDate;

    // Constructors
    public CandidateDTO() {
    }

    public CandidateDTO(String firstName, String lastName, String email, String phoneNumber, String cin,
                        LocalDate dateOfBirth, String currentTitle, String currentCompany,
                        List<String> skills, Integer yearsOfExperience) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.cin = cin;
        this.dateOfBirth = dateOfBirth;
        this.currentTitle = currentTitle;
        this.currentCompany = currentCompany;
        this.skills = skills;
        this.yearsOfExperience = yearsOfExperience;

    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getCin() {
        return cin;
    }

    public void setCin(String cin) {
        this.cin = cin;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getCurrentTitle() {
        return currentTitle;
    }

    public void setCurrentTitle(String currentTitle) {
        this.currentTitle = currentTitle;
    }

    public String getCurrentCompany() {
        return currentCompany;
    }

    public void setCurrentCompany(String currentCompany) {
        this.currentCompany = currentCompany;
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }

    public Integer getYearsOfExperience() {
        return yearsOfExperience;
    }

    public void setYearsOfExperience(Integer yearsOfExperience) {
        this.yearsOfExperience = yearsOfExperience;
    }

    public String getCvFileId() {
        return cvFileId;
    }

    public void setCvFileId(String cvFileId) {
        this.cvFileId = cvFileId;
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(LocalDateTime registrationDate) {
        this.registrationDate = registrationDate;
    }
}

