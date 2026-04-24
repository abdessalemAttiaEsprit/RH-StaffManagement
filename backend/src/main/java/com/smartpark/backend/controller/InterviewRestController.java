package com.smartpark.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.smartpark.backend.dto.InterviewDTO;
import com.smartpark.backend.service.IInterviewService;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.smartpark.backend.dto.InterviewDTO;

@RestController
@RequestMapping("/api/interviews")
@CrossOrigin(origins = "http://localhost:4200")
public class InterviewRestController {

    @Autowired
    private IInterviewService interviewService;

    @PostMapping
    public ResponseEntity<InterviewDTO> scheduleInterview(@RequestBody InterviewDTO interviewDTO) {
        System.out.println("🔴 DEBUG scheduleInterview - Creating new interview:");
        System.out.println("   applicationId: " + interviewDTO.getApplicationId());
        System.out.println("   candidateId: " + interviewDTO.getCandidateId());
        System.out.println("   interviewDate: " + interviewDTO.getInterviewDate());
        System.out.println("   interviewLocation: " + interviewDTO.getInterviewLocation());

        InterviewDTO created = interviewService.scheduleInterview(interviewDTO);

        System.out.println("🟢 DEBUG scheduleInterview - Created:");
        System.out.println("   id: " + created.getId());
        System.out.println("   interviewDate: " + created.getInterviewDate());
        System.out.println("   interviewLocation: " + created.getInterviewLocation());

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<InterviewDTO>> getAllInterviews() {
        List<InterviewDTO> interviews = interviewService.getAllInterviews();
        return ResponseEntity.ok(interviews);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InterviewDTO> getInterviewById(@PathVariable String id) {
        InterviewDTO interview = interviewService.getInterviewById(id);
        return ResponseEntity.ok(interview);
    }

    @GetMapping("/candidate/{candidateId}")
    public ResponseEntity<List<InterviewDTO>> getInterviewsByCandidate(@PathVariable String candidateId) {
        List<InterviewDTO> interviews = interviewService.getInterviewsByCandidate(candidateId);
        return ResponseEntity.ok(interviews);
    }

    @GetMapping("/application/{applicationId}")
    public ResponseEntity<List<InterviewDTO>> getInterviewsByApplication(@PathVariable String applicationId) {
        List<InterviewDTO> interviews = interviewService.getInterviewsByApplication(applicationId);
        return ResponseEntity.ok(interviews);
    }

    @GetMapping("/jobPosting/{jobPostingId}")
    public ResponseEntity<List<InterviewDTO>> getInterviewsByJobPosting(@PathVariable String jobPostingId) {
        List<InterviewDTO> interviews = interviewService.getInterviewsByJobPosting(jobPostingId);
        return ResponseEntity.ok(interviews);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<InterviewDTO>> getInterviewsByStatus(@PathVariable String status) {
        List<InterviewDTO> interviews = interviewService.getInterviewsByStatus(status);
        return ResponseEntity.ok(interviews);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<InterviewDTO> rescheduleInterview(
            @PathVariable String id,
            @RequestBody InterviewDTO interviewDTO) {
        InterviewDTO updated = interviewService.rescheduleInterview(id, interviewDTO);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<InterviewDTO> cancelInterview(@PathVariable String id) {
        InterviewDTO cancelled = interviewService.cancelInterview(id);
        return ResponseEntity.ok(cancelled);
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<InterviewDTO> completeInterview(@PathVariable String id) {
        InterviewDTO completed = interviewService.completeInterview(id);
        return ResponseEntity.ok(completed);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInterview(@PathVariable String id) {
        interviewService.deleteInterview(id);
        return ResponseEntity.noContent().build();
    }


}

