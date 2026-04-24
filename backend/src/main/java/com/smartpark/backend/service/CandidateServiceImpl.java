package com.smartpark.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.smartpark.backend.repository.ICandidateRepository;
import com.smartpark.backend.repository.IApplicationRepository;
import com.smartpark.backend.repository.InterviewRepository;
import com.smartpark.backend.dto.CandidateDTO;
import com.smartpark.backend.model.Candidate;
import com.smartpark.backend.exceptions.ResourceNotFoundException;
import com.smartpark.backend.mapper.CandidateMapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CandidateServiceImpl implements ICandidateService {
    
    @Autowired
    private ICandidateRepository candidateRepository;
    
    @Autowired
    private IApplicationRepository applicationRepository;
    
    @Autowired
    private InterviewRepository interviewRepository;
    
    @Autowired
    private CandidateMapper candidateMapper;
    
    @Autowired
    private ICvStorageService cvStorageService;
    
    @Override
    public CandidateDTO registerCandidate(CandidateDTO candidateDTO) {
        Candidate candidate = candidateMapper.toEntity(candidateDTO);
        candidate.setRegistrationDate(LocalDateTime.now());
        Candidate saved = candidateRepository.save(candidate);
        return candidateMapper.toDTO(saved);
    }
    
    @Override
    public List<CandidateDTO> getAllCandidates() {
        return candidateRepository.findAll()
                .stream()
                .map(candidateMapper::toDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    public CandidateDTO getCandidateById(String id) {
        Candidate candidate = candidateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found with id: " + id));
        return candidateMapper.toDTO(candidate);
    }
    
    @Override
    public CandidateDTO updateCandidate(String id, CandidateDTO candidateDTO) {
        Candidate candidate = candidateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found with id: " + id));
        
        if (candidateDTO.getFirstName() != null) {
            candidate.setFirstName(candidateDTO.getFirstName());
        }
        if (candidateDTO.getLastName() != null) {
            candidate.setLastName(candidateDTO.getLastName());
        }
        if (candidateDTO.getEmail() != null) {
            candidate.setEmail(candidateDTO.getEmail());
        }
        if (candidateDTO.getPhoneNumber() != null) {
            candidate.setPhoneNumber(candidateDTO.getPhoneNumber());
        }
        if (candidateDTO.getSkills() != null) {
            candidate.setSkills(candidateDTO.getSkills());
        }
        if (candidateDTO.getYearsOfExperience() != null) {
            candidate.setYearsOfExperience(candidateDTO.getYearsOfExperience());
        }

        Candidate updated = candidateRepository.save(candidate);
        return candidateMapper.toDTO(updated);
    }
    
    @Override
    @Transactional
    public void deleteCandidate(String id) {
        log.info("=== DELETE CANDIDATE START: {} ===", id);
        
        Candidate candidate = candidateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found with id: " + id));
        
        // Delete CV if exists
        if (candidate.getCvFileId() != null) {
            log.info("Deleting CV: {}", candidate.getCvFileId());
            cvStorageService.deleteCv(candidate.getCvFileId());
        }
        
        try {
            List<String> applicationIds = applicationRepository.findByCandidateId(id)
                    .stream()
                    .map(app -> app.getId())
                    .collect(Collectors.toList());
            
            log.info("Found {} applications for candidate: {}", applicationIds.size(), id);

            for (String appId : applicationIds) {
                long deletedInterviewsForApp = interviewRepository.deleteByApplicationId(appId);
                log.info("✓ Deleted {} interviews for application: {}", deletedInterviewsForApp, appId);
            }

            long deletedInterviewsDirect = interviewRepository.deleteByCandidateId(id);
            log.info("✓ Deleted {} interviews directly for candidate: {}", deletedInterviewsDirect, id);

            long deletedCount = applicationRepository.deleteByCandidateId(id);
            log.info("✓ Deleted {} applications", deletedCount);

            candidateRepository.deleteById(id);
            log.info("✓ Candidate {} deleted successfully", id);
        } catch (Exception e) {
            log.error("❌ Error deleting candidate {}: {}", id, e.getMessage());
            throw new RuntimeException("Failed to delete candidate and related data: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<CandidateDTO> findBySkills(List<String> skills) {
        return candidateRepository.findBySkills(skills)
                .stream()
                .map(candidateMapper::toDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    public CandidateDTO findByEmail(String email) {
        Candidate candidate = candidateRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found with email: " + email));
        return candidateMapper.toDTO(candidate);
    }
    
    @Override
    public CandidateDTO uploadCv(String candidateId, MultipartFile file) throws IOException {
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found with id: " + candidateId));
        
        // Delete old CV if exists
        if (candidate.getCvFileId() != null) {
            cvStorageService.deleteCv(candidate.getCvFileId());
        }
        
        // Store new CV in GridFS
        String fileId = cvStorageService.storeCv(candidateId, file);
        candidate.setCvFileId(fileId);
        
        Candidate updated = candidateRepository.save(candidate);
        return candidateMapper.toDTO(updated);
    }
    
    @Override
    public byte[] downloadCv(String candidateId) {
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found with id: " + candidateId));
        
        if (candidate.getCvFileId() == null) {
            throw new ResourceNotFoundException("No CV found for candidate with id: " + candidateId);
        }
        
        return cvStorageService.retrieveCv(candidate.getCvFileId());
    }
    
    @Override
    public void deleteCv(String candidateId) {
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found with id: " + candidateId));
        
        if (candidate.getCvFileId() != null) {
            cvStorageService.deleteCv(candidate.getCvFileId());
            candidate.setCvFileId(null);
            candidateRepository.save(candidate);
        }
    }
}

