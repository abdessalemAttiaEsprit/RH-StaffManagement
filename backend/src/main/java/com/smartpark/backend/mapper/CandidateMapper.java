package com.smartpark.backend.mapper;

import org.springframework.stereotype.Component;
import com.smartpark.backend.dto.CandidateDTO;
import com.smartpark.backend.model.Candidate;

@Component
public class CandidateMapper {
    
    public CandidateDTO toDTO(Candidate candidate) {
        if (candidate == null) {
            return null;
        }
        
        CandidateDTO dto = new CandidateDTO();
        dto.setId(candidate.getId());
        dto.setFirstName(candidate.getFirstName());
        dto.setLastName(candidate.getLastName());
        dto.setEmail(candidate.getEmail());
        dto.setPhoneNumber(candidate.getPhoneNumber());
        dto.setCin(candidate.getCin());
        dto.setDateOfBirth(candidate.getDateOfBirth());
        dto.setCurrentTitle(candidate.getCurrentTitle());
        dto.setCurrentCompany(candidate.getCurrentCompany());
        dto.setSkills(candidate.getSkills());
        dto.setYearsOfExperience(candidate.getYearsOfExperience());
        dto.setCvFileId(candidate.getCvFileId());
        dto.setRegistrationDate(candidate.getRegistrationDate());
        
        return dto;
    }
    
    public Candidate toEntity(CandidateDTO dto) {
        if (dto == null) {
            return null;
        }
        
        Candidate candidate = new Candidate();
        candidate.setId(dto.getId());
        candidate.setFirstName(dto.getFirstName());
        candidate.setLastName(dto.getLastName());
        candidate.setEmail(dto.getEmail());
        candidate.setPhoneNumber(dto.getPhoneNumber());
        candidate.setCin(dto.getCin());
        candidate.setDateOfBirth(dto.getDateOfBirth());
        candidate.setCurrentTitle(dto.getCurrentTitle());
        candidate.setCurrentCompany(dto.getCurrentCompany());
        candidate.setSkills(dto.getSkills());
        candidate.setYearsOfExperience(dto.getYearsOfExperience());
        candidate.setCvFileId(dto.getCvFileId());
        candidate.setRegistrationDate(dto.getRegistrationDate());
        
        return candidate;
    }
}

