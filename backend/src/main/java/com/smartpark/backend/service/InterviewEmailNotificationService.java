package com.smartpark.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import com.smartpark.backend.model.Candidate;
import com.smartpark.backend.model.Interview;
import com.smartpark.backend.model.JobPosting;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewEmailNotificationService {

    private final JavaMailSender mailSender;

    @Value("${notifications.mail.enabled:true}")
    private boolean mailEnabled;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${notifications.mail.recruitment.cc:}")
    private String recruitmentCc;

    public void notifyInterviewScheduled(Candidate candidate, JobPosting jobPosting, Interview interview) {
        if (!mailEnabled) {
            return;
        }
        if (candidate == null || interview == null) {
            return;
        }
        if (mailHost == null || mailHost.isBlank()) {
            log.warn("Mail not configured (spring.mail.host missing). Skipping interview scheduled email.");
            return;
        }

        String to = candidate.getEmail();
        if (to == null || to.isBlank() || hasCrlf(to)) {
            log.warn("Candidate email missing/invalid for candidateId {}. Skipping interview scheduled email.", candidate.getId());
            return;
        }

        String fullName = (candidate.getFirstName() != null ? candidate.getFirstName() : "")
                + (candidate.getLastName() != null ? (" " + candidate.getLastName()) : "");
        fullName = fullName.trim();
        if (fullName.isBlank()) {
            fullName = "candidat";
        }

        String jobTitle = (jobPosting != null && jobPosting.getTitle() != null) ? jobPosting.getTitle().trim() : "";

        LocalDateTime dateTime = interview.getInterviewDate();
        String dateLabel = dateTime != null
                ? dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : "(non précisée)";

        String location = interview.getInterviewLocation() != null ? interview.getInterviewLocation().trim() : "";
        if (location.isBlank()) {
            location = "(non précisé)";
        }

        String subject = "Entretien programmé" + (!jobTitle.isBlank() ? (" - " + jobTitle) : "");
        subject = stripCrlf(subject);

        String body = "Bonjour " + fullName + ",\n\n"
                + "Votre entretien" + (!jobTitle.isBlank() ? (" pour le poste \"" + jobTitle + "\"") : "") + " a été programmé.\n\n"
                + "Date/heure : " + dateLabel + "\n"
                + "Lieu      : " + location + "\n\n"
                + "Si vous êtes intéressé par cet entretien, merci de bien vouloir confirmer votre présence par mail."
                + "Cordialement,\n"
                + "SmartPark RH";

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (fromAddress != null && !fromAddress.isBlank() && !hasCrlf(fromAddress)) {
                message.setFrom(fromAddress);
            }
            message.setTo(to);

            String[] ccList = parseRecipients(recruitmentCc);
            if (ccList.length > 0) {
                message.setCc(ccList);
            }

            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.info("Interview scheduled email sent to {} (cc={}, interviewId={})", to, ccList.length, interview.getId());
        } catch (Exception e) {
            // On ne bloque pas la planification de l'entretien si l'email échoue.
            log.warn("Failed to send interview scheduled email to {} (interviewId={}). Cause: {}",
                    to, interview.getId(), e.getMessage());
        }
    }

    private static String[] parseRecipients(String raw) {
        if (raw == null) {
            return new String[0];
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return new String[0];
        }

        String[] parts = trimmed.split("[;,]");
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String p : parts) {
            if (p == null) {
                continue;
            }
            String v = p.trim();
            if (v.isEmpty()) {
                continue;
            }
            if (hasCrlf(v)) {
                continue;
            }
            out.add(v);
        }
        return out.toArray(new String[0]);
    }

    private static boolean hasCrlf(String value) {
        return value != null && (value.contains("\r") || value.contains("\n"));
    }

    private static String stripCrlf(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("\r", "").replace("\n", "");
    }
}

