package com.smartpark.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.smartpark.backend.dto.ApplicationDTO;
import com.smartpark.backend.dto.InterviewDTO;
import com.smartpark.backend.service.IApplicationService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/applications")
@CrossOrigin(origins = "http://localhost:4200")
public class ApplicationRestController {
    
    @Autowired
    private IApplicationService applicationService;
    

    @PostMapping
    public ResponseEntity<ApplicationDTO> applyForJob(@RequestBody ApplicationDTO applicationDTO) {
        ApplicationDTO application = applicationService.applyForJob(applicationDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(application);
    }
    

    @GetMapping
    public ResponseEntity<List<ApplicationDTO>> getAllApplications() {
        List<ApplicationDTO> applications = applicationService.getAllApplications();
        return ResponseEntity.ok(applications);
    }
    

    @GetMapping("/{id}")
    public ResponseEntity<ApplicationDTO> getApplicationById(@PathVariable String id) {
        ApplicationDTO application = applicationService.getApplicationById(id);
        return ResponseEntity.ok(application);
    }
    

    @GetMapping("/jobPosting/{jobPostingId}")
    public ResponseEntity<List<ApplicationDTO>> getByJobPosting(@PathVariable String jobPostingId) {
        List<ApplicationDTO> applications = applicationService.getApplicationsByJobPosting(jobPostingId);
        return ResponseEntity.ok(applications);
    }
    

    @GetMapping("/candidate/{candidateId}")
    public ResponseEntity<List<ApplicationDTO>> getByCandidate(@PathVariable String candidateId) {
        List<ApplicationDTO> applications = applicationService.getApplicationsByCandidate(candidateId);
        return ResponseEntity.ok(applications);
    }
    

    @GetMapping("/status/{status}")
    public ResponseEntity<List<ApplicationDTO>> getByStatus(@PathVariable String status) {
        List<ApplicationDTO> applications = applicationService.getApplicationsByStatus(status);
        return ResponseEntity.ok(applications);
    }
    

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApplicationDTO> updateStatus(@PathVariable String id, @RequestParam String status) {
        ApplicationDTO updated = applicationService.updateApplicationStatus(id, status);
        return ResponseEntity.ok(updated);
    }
    

    @PatchMapping("/{id}/score")
    public ResponseEntity<ApplicationDTO> scoreApplication(@PathVariable String id, @RequestParam Double score,
                                                           @RequestParam(required = false) String feedback) {
        ApplicationDTO updated = applicationService.scoreApplication(id, score, feedback);
        return ResponseEntity.ok(updated);
    }


    @PostMapping("/{id}/ai-score")
    public ResponseEntity<?> scoreApplicationWithAI(@PathVariable String id) {
        try {
            ApplicationDTO updated = applicationService.scoreApplicationWithAI(id);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Map.of("error", e.getMessage()));
        }
    }
    

    @PatchMapping("/{id}/reject")
    public ResponseEntity<Void> rejectApplication(@PathVariable String id, @RequestParam(required = false) String reason) {
        applicationService.rejectApplication(id, reason);
        return ResponseEntity.noContent().build();
    }
    

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(@PathVariable String id) {
        applicationService.deleteApplication(id);
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/{id}/interview")
    public ResponseEntity<ApplicationDTO> scheduleInterview(
            @PathVariable String id,
            @RequestBody InterviewDTO interviewDTO) {
        
        System.out.println("🔴 DEBUG scheduleInterview - Received:");
        System.out.println("   applicationId: " + id);
        System.out.println("   interviewDate: " + interviewDTO.getInterviewDate());
        System.out.println("   interviewLocation: " + interviewDTO.getInterviewLocation());
        
        ApplicationDTO updated = applicationService.scheduleInterview(
            id, 
            interviewDTO.getInterviewDate(), 
            interviewDTO.getInterviewLocation()
        );
        
        System.out.println("🟢 DEBUG scheduleInterview - Returning:");
        System.out.println("   interviewDate: " + updated.getInterviewDate());
        System.out.println("   interviewLocation: " + updated.getInterviewLocation());
        
        return ResponseEntity.ok(updated);
    }


    @PatchMapping("/{id}/interview")
    public ResponseEntity<ApplicationDTO> rescheduleInterview(
            @PathVariable String id,
            @RequestBody InterviewDTO interviewDTO) {
        
        ApplicationDTO updated = applicationService.rescheduleInterview(
            id, 
            interviewDTO.getInterviewDate(), 
            interviewDTO.getInterviewLocation()
        );
        return ResponseEntity.ok(updated);
    }


    @DeleteMapping("/{id}/interview")
    public ResponseEntity<ApplicationDTO> cancelInterview(@PathVariable String id) {
        ApplicationDTO updated = applicationService.cancelInterview(id);
        return ResponseEntity.ok(updated);
    }


    @GetMapping("/interviews/scheduled")
    public ResponseEntity<List<ApplicationDTO>> getScheduledInterviews() {
        List<ApplicationDTO> interviews = applicationService.getScheduledInterviews();
        return ResponseEntity.ok(interviews);
    }


    @GetMapping("/interviews/upcoming")
    public ResponseEntity<List<ApplicationDTO>> getUpcomingInterviews(@RequestParam(required = false) Integer days) {
        List<ApplicationDTO> interviews = applicationService.getUpcomingInterviews(days);
        return ResponseEntity.ok(interviews);
    }
}

