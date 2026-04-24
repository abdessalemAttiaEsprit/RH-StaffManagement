package com.smartpark.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.smartpark.backend.repository.ICandidateRepository;
import com.smartpark.backend.repository.IJobPostingRepository;
import com.smartpark.backend.repository.InterviewRepository;
import com.smartpark.backend.dto.InterviewDTO;
import com.smartpark.backend.model.Candidate;
import com.smartpark.backend.model.Interview;
import com.smartpark.backend.model.JobPosting;
import com.smartpark.backend.exceptions.ResourceNotFoundException;
import com.smartpark.backend.mapper.InterviewMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InterviewServiceImpl implements IInterviewService {

    @Autowired
    private InterviewRepository interviewRepository;

    @Autowired
    private InterviewMapper interviewMapper;

    @Autowired
    private ICandidateRepository candidateRepository;

    @Autowired
    private IJobPostingRepository jobPostingRepository;

    @Autowired
    private InterviewEmailNotificationService interviewEmailNotificationService;

    @Override
    public InterviewDTO scheduleInterview(InterviewDTO interviewDTO) {
        Interview interview = interviewMapper.toEntity(interviewDTO);
        interview.setCreatedAt(LocalDateTime.now());
        interview.setStatus("SCHEDULED");
        Interview saved = interviewRepository.save(interview);

        Candidate candidate = null;
        if (saved.getCandidateId() != null && !saved.getCandidateId().isBlank()) {
            candidate = candidateRepository.findById(saved.getCandidateId()).orElse(null);
        }

        JobPosting jobPosting = null;
        if (saved.getJobPostingId() != null && !saved.getJobPostingId().isBlank()) {
            jobPosting = jobPostingRepository.findById(saved.getJobPostingId()).orElse(null);
        }

        interviewEmailNotificationService.notifyInterviewScheduled(candidate, jobPosting, saved);

        return interviewMapper.toDTO(saved);
    }

    @Override
    public InterviewDTO rescheduleInterview(String interviewId, InterviewDTO interviewDTO) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found with id: " + interviewId));
        
        interview.setInterviewDate(interviewDTO.getInterviewDate());
        interview.setInterviewLocation(interviewDTO.getInterviewLocation());
        
        Interview updated = interviewRepository.save(interview);
        return interviewMapper.toDTO(updated);
    }

    @Override
    public InterviewDTO cancelInterview(String interviewId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found with id: " + interviewId));
        
        interview.setStatus("CANCELLED");
        Interview updated = interviewRepository.save(interview);
        return interviewMapper.toDTO(updated);
    }

    @Override
    public InterviewDTO completeInterview(String interviewId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found with id: " + interviewId));
        
        interview.setStatus("COMPLETED");
        Interview updated = interviewRepository.save(interview);
        return interviewMapper.toDTO(updated);
    }

    @Override
    public InterviewDTO getInterviewById(String interviewId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found with id: " + interviewId));
        return interviewMapper.toDTO(interview);
    }

    @Override
    public List<InterviewDTO> getAllInterviews() {
        return interviewRepository.findAll()
                .stream()
                .map(interviewMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<InterviewDTO> getInterviewsByCandidate(String candidateId) {
        return interviewRepository.findByCandidateId(candidateId)
                .stream()
                .map(interviewMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<InterviewDTO> getInterviewsByApplication(String applicationId) {
        return interviewRepository.findByApplicationId(applicationId)
                .stream()
                .map(interviewMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<InterviewDTO> getInterviewsByJobPosting(String jobPostingId) {
        return interviewRepository.findByJobPostingId(jobPostingId)
                .stream()
                .map(interviewMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<InterviewDTO> getInterviewsByStatus(String status) {
        return interviewRepository.findByStatus(status)
                .stream()
                .map(interviewMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteInterview(String interviewId) {
        if (!interviewRepository.existsById(interviewId)) {
            throw new ResourceNotFoundException("Interview not found with id: " + interviewId);
        }
        interviewRepository.deleteById(interviewId);
    }
}

