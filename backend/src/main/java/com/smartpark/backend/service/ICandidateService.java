package com.smartpark.backend.service;

import org.springframework.web.multipart.MultipartFile;
import com.smartpark.backend.dto.CandidateDTO;

import java.io.IOException;
import java.util.List;

public interface ICandidateService {
    
    CandidateDTO registerCandidate(CandidateDTO candidateDTO);
    
    List<CandidateDTO> getAllCandidates();
    
    CandidateDTO getCandidateById(String id);
    
    CandidateDTO updateCandidate(String id, CandidateDTO candidateDTO);
    
    void deleteCandidate(String id);
    
    List<CandidateDTO> findBySkills(List<String> skills);
    
    CandidateDTO findByEmail(String email);
    
    /**
     * Upload CV for a candidate
     * @param candidateId the candidate ID
     * @param file the PDF file
     * @return the updated candidate DTO
     * @throws IOException if there's an error reading the file
     */
    CandidateDTO uploadCv(String candidateId, MultipartFile file) throws IOException;
    
    /**
     * Download CV for a candidate
     * @param candidateId the candidate ID
     * @return the file content as byte array
     */
    byte[] downloadCv(String candidateId);
    
    /**
     * Delete CV for a candidate
     * @param candidateId the candidate ID
     */
    void deleteCv(String candidateId);
}

