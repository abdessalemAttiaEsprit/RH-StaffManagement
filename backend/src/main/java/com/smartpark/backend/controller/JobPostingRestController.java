package com.smartpark.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.smartpark.backend.dto.JobPostingDTO;
import com.smartpark.backend.service.IJobPostingService;

import java.util.List;

@RestController
@RequestMapping("/api/jobPostings")
@CrossOrigin(origins = "http://localhost:4200")
public class JobPostingRestController {
    
    @Autowired
    private IJobPostingService jobPostingService;

    @PostMapping
    public ResponseEntity<JobPostingDTO> createJobPosting(@RequestBody JobPostingDTO jobPostingDTO) {
        JobPostingDTO created = jobPostingService.createJobPosting(jobPostingDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<JobPostingDTO>> getAllJobPostings() {
        List<JobPostingDTO> postings = jobPostingService.getAllJobPostings();
        return ResponseEntity.ok(postings);
    }

    @GetMapping("/open")
    public ResponseEntity<List<JobPostingDTO>> getOpenJobPostings() {
        List<JobPostingDTO> postings = jobPostingService.getOpenJobPostings();
        return ResponseEntity.ok(postings);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobPostingDTO> getJobPostingById(@PathVariable String id) {
        JobPostingDTO posting = jobPostingService.getJobPostingById(id);
        return ResponseEntity.ok(posting);
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobPostingDTO> updateJobPosting(@PathVariable String id, @RequestBody JobPostingDTO jobPostingDTO) {
        JobPostingDTO updated = jobPostingService.updateJobPosting(id, jobPostingDTO);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/close")
    public ResponseEntity<Void> closeJobPosting(@PathVariable String id) {
        jobPostingService.closeJobPosting(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJobPosting(@PathVariable String id) {
        jobPostingService.deleteJobPosting(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/department/{department}")
    public ResponseEntity<List<JobPostingDTO>> getByDepartment(@PathVariable String department) {
        List<JobPostingDTO> postings = jobPostingService.findByDepartment(department);
        return ResponseEntity.ok(postings);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<JobPostingDTO>> getByStatus(@PathVariable String status) {
        List<JobPostingDTO> postings = jobPostingService.findByStatus(status);
        return ResponseEntity.ok(postings);
    }
}

