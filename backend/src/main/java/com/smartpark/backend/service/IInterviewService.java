package com.smartpark.backend.service;

import com.smartpark.backend.dto.InterviewDTO;
import java.util.List;

public interface IInterviewService {
    InterviewDTO scheduleInterview(InterviewDTO interviewDTO);
    InterviewDTO rescheduleInterview(String interviewId, InterviewDTO interviewDTO);
    InterviewDTO cancelInterview(String interviewId);
    InterviewDTO completeInterview(String interviewId);
    InterviewDTO getInterviewById(String interviewId);
    List<InterviewDTO> getAllInterviews();
    List<InterviewDTO> getInterviewsByCandidate(String candidateId);
    List<InterviewDTO> getInterviewsByApplication(String applicationId);
    List<InterviewDTO> getInterviewsByJobPosting(String jobPostingId);
    List<InterviewDTO> getInterviewsByStatus(String status);
    void deleteInterview(String interviewId);
}

