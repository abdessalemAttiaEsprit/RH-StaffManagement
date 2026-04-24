package com.smartpark.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.smartpark.backend.repository.IApplicationRepository;
import com.smartpark.backend.repository.ICandidateRepository;
import com.smartpark.backend.repository.IJobPostingRepository;
import com.smartpark.backend.repository.InterviewRepository;
import com.smartpark.backend.dto.ApplicationDTO;
import com.smartpark.backend.model.Application;
import com.smartpark.backend.model.Candidate;
import com.smartpark.backend.model.JobPosting;
import com.smartpark.backend.exceptions.ResourceNotFoundException;
import com.smartpark.backend.mapper.ApplicationMapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Comparator;

@Service
@Slf4j
public class ApplicationServiceImpl implements IApplicationService {
    
    @Autowired
    private IApplicationRepository applicationRepository;
    
    @Autowired
    private IJobPostingRepository jobPostingRepository;
    
    @Autowired
    private InterviewRepository interviewRepository;
    
    @Autowired
    private ApplicationMapper applicationMapper;

    @Autowired
    private ICandidateRepository candidateRepository;

    @Autowired
    private ICvStorageService cvStorageService;

    @Autowired
    private IRecruitingAIService recruitingAIService;
    
    @Override
    public ApplicationDTO applyForJob(ApplicationDTO applicationDTO) {
        Application application = applicationMapper.toEntity(applicationDTO);
        application.setAppliedDate(LocalDateTime.now());
        if (application.getStatus() == null) {
            application.setStatus("SUBMITTED");
        }
        
        Application saved = applicationRepository.save(application);
        JobPosting jobPosting = jobPostingRepository.findById(applicationDTO.getJobPostingId())
                .orElseThrow(() -> new ResourceNotFoundException("Job Posting not found"));
        Integer count = jobPosting.getApplicationsCount() != null ? jobPosting.getApplicationsCount() : 0;
        jobPosting.setApplicationsCount(count + 1);
        jobPostingRepository.save(jobPosting);
        try {
            Candidate candidate = candidateRepository.findById(applicationDTO.getCandidateId())
                    .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));

            if (candidate.getCvFileId() == null) {
                log.warn("AI scoring skipped: no CV for candidate {}", candidate.getId());
                return applicationMapper.toDTO(saved);
            }

            if (!cvStorageService.cvExists(candidate.getCvFileId())) {
                log.warn("AI scoring skipped: CV file not found in GridFS: {}", candidate.getCvFileId());
                return applicationMapper.toDTO(saved);
            }

            String jobDescription = buildJobDescription(jobPosting);
            if (jobDescription.trim().isEmpty()) {
                log.warn("AI scoring skipped: job description is empty for posting {}", jobPosting.getId());
                return applicationMapper.toDTO(saved);
            }

            byte[] cvBytes = cvStorageService.retrieveCv(candidate.getCvFileId());
            java.util.Map<String, Object> aiResult = recruitingAIService.evaluateCandidateMatch(jobDescription, cvBytes);

            Double aiScore = parseScore(aiResult.get("score"));
            String aiFeedback = aiResult.get("feedback") != null ? aiResult.get("feedback").toString() : null;

            if (aiScore != null) {
                saved.setAiScore(aiScore);
                saved.setAiFeedback(aiFeedback);
                saved.setEvaluatedAt(LocalDateTime.now());

                saved.setScore(aiScore);
                saved.setFeedback(aiFeedback);

                saved = applicationRepository.save(saved);
            }
        } catch (Exception e) {
            log.warn("AI scoring failed for application {}: {}", saved.getId(), e.getMessage());
        }
        
        return applicationMapper.toDTO(saved);
    }

    private String buildJobDescription(JobPosting jobPosting) {
        StringBuilder sb = new StringBuilder();
        if (jobPosting.getTitle() != null && !jobPosting.getTitle().trim().isEmpty()) {
            sb.append("Poste: ").append(jobPosting.getTitle().trim()).append("\n");
        }
        if (jobPosting.getDescription() != null && !jobPosting.getDescription().trim().isEmpty()) {
            sb.append(jobPosting.getDescription().trim()).append("\n");
        }
        if (jobPosting.getRequiredSkills() != null && !jobPosting.getRequiredSkills().isEmpty()) {
            sb.append("Competences requises: ")
              .append(String.join(", ", jobPosting.getRequiredSkills()))
              .append("\n");
        }
        return sb.toString();
    }

    private Double parseScore(Object scoreObj) {
        if (scoreObj instanceof Number) {
            return ((Number) scoreObj).doubleValue();
        }
        if (scoreObj != null) {
            try {
                return Double.parseDouble(scoreObj.toString());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
    
    @Override
    public List<ApplicationDTO> getAllApplications() {
        return applicationRepository.findAll()
                .stream()
                .map(applicationMapper::toDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    public ApplicationDTO getApplicationById(String id) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));
        return applicationMapper.toDTO(application);
    }
    
    @Override
    public List<ApplicationDTO> getApplicationsByJobPosting(String jobPostingId) {
        return applicationRepository.findByJobPostingId(jobPostingId)
                .stream()
                .map(applicationMapper::toDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ApplicationDTO> getApplicationsByCandidate(String candidateId) {
        return applicationRepository.findByCandidateId(candidateId)
                .stream()
                .map(applicationMapper::toDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ApplicationDTO> getApplicationsByStatus(String status) {
        return applicationRepository.findByStatus(status)
                .stream()
                .map(applicationMapper::toDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    public ApplicationDTO updateApplicationStatus(String id, String status) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));
        application.setStatus(status);
        Application updated = applicationRepository.save(application);
        return applicationMapper.toDTO(updated);
    }
    
    @Override
    public ApplicationDTO scoreApplication(String id, Double score, String feedback) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));
        application.setScore(score);
        application.setFeedback(feedback);
        application.setStatus("UNDER_REVIEW");
        Application updated = applicationRepository.save(application);
        return applicationMapper.toDTO(updated);
    }

    @Override
    public ApplicationDTO scoreApplicationWithAI(String id) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));

        JobPosting jobPosting = jobPostingRepository.findById(application.getJobPostingId())
                .orElseThrow(() -> new ResourceNotFoundException("Job Posting not found"));

        Candidate candidate = candidateRepository.findById(application.getCandidateId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));

        if (candidate.getCvFileId() == null || candidate.getCvFileId().isBlank()) {
            throw new IllegalArgumentException("Aucun CV (cvFileId) trouvé pour ce candidat");
        }
        if (!cvStorageService.cvExists(candidate.getCvFileId())) {
            throw new IllegalArgumentException("CV introuvable dans GridFS: " + candidate.getCvFileId());
        }

        String jobDescription = buildJobDescription(jobPosting);
        if (jobDescription.isBlank()) {
            throw new IllegalArgumentException("Description du poste vide");
        }

        try {
            byte[] cvBytes = cvStorageService.retrieveCv(candidate.getCvFileId());
            java.util.Map<String, Object> aiResult = recruitingAIService.evaluateCandidateMatch(jobDescription, cvBytes);

            Double aiScore = parseScore(aiResult.get("score"));
            String aiFeedback = aiResult.get("feedback") != null ? aiResult.get("feedback").toString() : null;

            if (aiScore == null) {
                throw new IllegalArgumentException("Score IA invalide");
            }

            application.setAiScore(aiScore);
            application.setAiFeedback(aiFeedback);
            application.setEvaluatedAt(LocalDateTime.now());

            application.setScore(aiScore);
            application.setFeedback(aiFeedback);
            application.setStatus("UNDER_REVIEW");

            Application updated = applicationRepository.save(application);
            return applicationMapper.toDTO(updated);
        } catch (IOException e) {
            throw new IllegalArgumentException("Erreur lecture CV PDF: " + e.getMessage());
        }
    }
    
    @Override
    public void rejectApplication(String id, String reason) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));
        application.setStatus("REJECTED");
        application.setFeedback(reason);
        applicationRepository.save(application);
    }
    
    @Override
    @Transactional
    public void deleteApplication(String id) {
        log.info("=== DELETE APPLICATION START: {} ===", id);
        
        if (!applicationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Application not found with id: " + id);
        }
        
        try {

            long deletedInterviews = interviewRepository.deleteByApplicationId(id);
            log.info("✓ Deleted {} interviews for application: {}", deletedInterviews, id);

            applicationRepository.deleteById(id);
            log.info("✓ Application {} deleted successfully", id);
        } catch (Exception e) {
            log.error("❌ Error deleting application {}: {}", id, e.getMessage());
            throw new RuntimeException("Failed to delete application and related data: " + e.getMessage(), e);
        }
    }

    @Override
    public ApplicationDTO scheduleInterview(String id, LocalDateTime interviewDate, String interviewLocation) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));
        
        application.setInterviewDate(interviewDate);
        application.setInterviewLocation(interviewLocation);
        application.setStatus("UNDER_REVIEW");
        
        Application updated = applicationRepository.save(application);
        return applicationMapper.toDTO(updated);
    }

    @Override
    public ApplicationDTO rescheduleInterview(String id, LocalDateTime newInterviewDate, String newInterviewLocation) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));
        
        if (application.getInterviewDate() == null) {
            throw new ResourceNotFoundException("No interview scheduled for this application");
        }
        
        application.setInterviewDate(newInterviewDate);
        application.setInterviewLocation(newInterviewLocation);
        
        Application updated = applicationRepository.save(application);
        return applicationMapper.toDTO(updated);
    }

    @Override
    public ApplicationDTO cancelInterview(String id) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));
        
        application.setInterviewDate(null);
        application.setInterviewLocation(null);
        
        Application updated = applicationRepository.save(application);
        return applicationMapper.toDTO(updated);
    }

    @Override
    public List<ApplicationDTO> getScheduledInterviews() {
        return applicationRepository.findAll()
                .stream()
                .filter(app -> app.getInterviewDate() != null)
                .sorted(Comparator.comparing(Application::getInterviewDate))
                .map(applicationMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ApplicationDTO> getUpcomingInterviews(Integer days) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime future = now.plusDays(days != null ? days : 7);
        
        return applicationRepository.findAll()
                .stream()
                .filter(app -> app.getInterviewDate() != null 
                    && app.getInterviewDate().isAfter(now) 
                    && app.getInterviewDate().isBefore(future))
                .sorted(Comparator.comparing(Application::getInterviewDate))
                .map(applicationMapper::toDTO)
                .collect(Collectors.toList());
    }
}

