package com.smartpark.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.smartpark.backend.repository.IJobPostingRepository;
import com.smartpark.backend.dto.JobPostingDTO;
import com.smartpark.backend.model.JobPosting;
import com.smartpark.backend.exceptions.ResourceNotFoundException;
import com.smartpark.backend.mapper.JobPostingMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JobPostingServiceImpl implements IJobPostingService {
    
    @Autowired
    private IJobPostingRepository jobPostingRepository;
    
    @Autowired
    private JobPostingMapper jobPostingMapper;
    
    @Override
    public JobPostingDTO createJobPosting(JobPostingDTO jobPostingDTO) {
        JobPosting jobPosting = jobPostingMapper.toEntity(jobPostingDTO);
        if (jobPosting.getDatePosted() == null) {
            jobPosting.setDatePosted(LocalDateTime.now());
        }
        if (jobPosting.getApplicationsCount() == null) {
            jobPosting.setApplicationsCount(0);
        }
        JobPosting saved = jobPostingRepository.save(jobPosting);
        return jobPostingMapper.toDTO(saved);
    }
    
    @Override
    public List<JobPostingDTO> getAllJobPostings() {
        return jobPostingRepository.findAll()
                .stream()
                .map(jobPostingMapper::toDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<JobPostingDTO> getOpenJobPostings() {
        // Récupérer toutes les offres avec statut OPEN
        List<JobPosting> postings = jobPostingRepository.findByStatus("OPEN");
        
        // Vérifier les dates limites et fermer les offres expirées
        LocalDateTime now = LocalDateTime.now();
        postings.forEach(posting -> {
            if (posting.getDeadline() != null && posting.getDeadline().isBefore(now)) {
                posting.setStatus("CLOSED");
                jobPostingRepository.save(posting);
                System.out.println("✓ Offre fermée automatiquement: " + posting.getTitle() + " (deadline: " + posting.getDeadline() + ")");
            }
        });
        
        // Retourner les offres réellement ouvertes après fermeture des expirées
        return jobPostingRepository.findByStatus("OPEN")
                .stream()
                .map(jobPostingMapper::toDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    public JobPostingDTO getJobPostingById(String id) {
        JobPosting jobPosting = jobPostingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job Posting not found with id: " + id));
        return jobPostingMapper.toDTO(jobPosting);
    }
    
    @Override
    public JobPostingDTO updateJobPosting(String id, JobPostingDTO jobPostingDTO) {
        JobPosting jobPosting = jobPostingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job Posting not found with id: " + id));
        
        if (jobPostingDTO.getTitle() != null) {
            jobPosting.setTitle(jobPostingDTO.getTitle());
        }
        if (jobPostingDTO.getDescription() != null) {
            jobPosting.setDescription(jobPostingDTO.getDescription());
        }
        if (jobPostingDTO.getDepartment() != null) {
            jobPosting.setDepartment(jobPostingDTO.getDepartment());
        }
        if (jobPostingDTO.getRequiredSkills() != null) {
            jobPosting.setRequiredSkills(jobPostingDTO.getRequiredSkills());
        }
        if (jobPostingDTO.getSalaryMin() != null) {
            jobPosting.setSalaryMin(jobPostingDTO.getSalaryMin());
        }
        if (jobPostingDTO.getSalaryMax() != null) {
            jobPosting.setSalaryMax(jobPostingDTO.getSalaryMax());
        }
        if (jobPostingDTO.getJobType() != null) {
            jobPosting.setJobType(jobPostingDTO.getJobType());
        }
        if (jobPostingDTO.getDeadline() != null) {
            jobPosting.setDeadline(jobPostingDTO.getDeadline());
        }
        if (jobPostingDTO.getStatus() != null) {
            jobPosting.setStatus(jobPostingDTO.getStatus());
        }
        if (jobPostingDTO.getNumberOfPositions() != null) {
            jobPosting.setNumberOfPositions(jobPostingDTO.getNumberOfPositions());
        }
        
        JobPosting updated = jobPostingRepository.save(jobPosting);
        return jobPostingMapper.toDTO(updated);
    }
    
    @Override
    public void closeJobPosting(String id) {
        JobPosting jobPosting = jobPostingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job Posting not found with id: " + id));
        jobPosting.setStatus("CLOSED");
        jobPostingRepository.save(jobPosting);
    }
    
    @Override
    public void deleteJobPosting(String id) {
        if (!jobPostingRepository.existsById(id)) {
            throw new ResourceNotFoundException("Job Posting not found with id: " + id);
        }
        jobPostingRepository.deleteById(id);
    }
    
    @Override
    public List<JobPostingDTO> findByDepartment(String department) {
        return jobPostingRepository.findByDepartment(department)
                .stream()
                .map(jobPostingMapper::toDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<JobPostingDTO> findByStatus(String status) {
        return jobPostingRepository.findByStatus(status)
                .stream()
                .map(jobPostingMapper::toDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<JobPostingDTO> findBySkills(List<String> skills) {
        return jobPostingRepository.findBySkills(skills)
                .stream()
                .map(jobPostingMapper::toDTO)
                .collect(Collectors.toList());
    }
}

