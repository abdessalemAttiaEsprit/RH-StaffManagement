package com.smartpark.backend.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.smartpark.backend.exceptions.GeminiRateLimitException;
import com.smartpark.backend.service.IRecruitingAIService;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "http://localhost:4200")
@Slf4j
public class RecruitingAIController {

    @Autowired
    private IRecruitingAIService recruitingAIService;

    @PostMapping(value = "/evaluate-candidate-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> evaluateCandidateWithPdf(
            @RequestParam(value = "jobDescription", required = false) String jobDescription,
            @RequestParam(value = "jobDesc", required = false) String jobDesc,
            @RequestParam(value = "cvPdf", required = false) MultipartFile cvPdf,
            @RequestParam(value = "cvPdfFile", required = false) MultipartFile cvPdfFile) {

        log.info("=== Évaluation d'un candidat avec PDF reçue ===");

        try {
            String finalJobDescription = (jobDescription != null && !jobDescription.isBlank())
                ? jobDescription
                : (jobDesc != null ? jobDesc : "");

            MultipartFile finalCvPdf = (cvPdf != null && !cvPdf.isEmpty()) ? cvPdf : cvPdfFile;

            if (finalJobDescription == null || finalJobDescription.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "jobDescription est requis"));
            }
            if (finalCvPdf == null || finalCvPdf.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "cvPdf (fichier PDF) est requis"));
            }

            Map<String, Object> result = recruitingAIService.evaluateCandidateMatch(
                finalJobDescription,
                finalCvPdf
            );
            return ResponseEntity.ok(result);
        } catch (GeminiRateLimitException e) {
            return ResponseEntity.status(429)
                .body(Map.of(
                    "error", "TOO_MANY_REQUESTS",
                    "message", e.getMessage(),
                    "retryAfterSeconds", e.getRetryAfterSeconds()
                ));
        } catch (IOException e) {
            log.error("Erreur lors de l'extraction du PDF", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Erreur lors de la lecture du PDF: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Erreur lors de l'évaluation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de l'évaluation: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/evaluate-candidate-pdf", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> evaluateCandidateWithPdfBase64(
            @RequestBody PdfEvaluationRequest requestBody) {

        log.info("=== Évaluation d'un candidat avec PDF Base64 reçue ===");

        try {
            String finalJobDescription = (requestBody.getJobDescription() != null && !requestBody.getJobDescription().isBlank())
                ? requestBody.getJobDescription()
                : (requestBody.getJobDesc() != null ? requestBody.getJobDesc() : "");

            String base64 = (requestBody.getCvPdfBase64() != null && !requestBody.getCvPdfBase64().isBlank())
                ? requestBody.getCvPdfBase64()
                : requestBody.getCvPdf();

            if (finalJobDescription == null || finalJobDescription.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "jobDescription est requis"));
            }
            if (base64 == null || base64.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "cvPdfBase64 (string base64 du PDF) est requis"));
            }

            // Supporte les data URLs: data:application/pdf;base64,....
            String sanitized = base64;
            int comma = sanitized.indexOf(',');
            if (comma >= 0 && sanitized.substring(0, comma).toLowerCase().contains("base64")) {
                sanitized = sanitized.substring(comma + 1);
            }

            byte[] pdfBytes;
            try {
                pdfBytes = Base64.getDecoder().decode(sanitized);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "cvPdfBase64 invalide: impossible de décoder le Base64"));
            }

            if (pdfBytes.length == 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "PDF vide"));
            }

            Map<String, Object> result = recruitingAIService.evaluateCandidateMatch(finalJobDescription, pdfBytes);
            return ResponseEntity.ok(result);
        } catch (GeminiRateLimitException e) {
            return ResponseEntity.status(429)
                .body(Map.of(
                    "error", "TOO_MANY_REQUESTS",
                    "message", e.getMessage(),
                    "retryAfterSeconds", e.getRetryAfterSeconds()
                ));
        } catch (IOException e) {
            log.error("Erreur lors de l'extraction du PDF", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Erreur lors de la lecture du PDF: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Erreur lors de l'évaluation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Erreur lors de l'évaluation: " + e.getMessage()));
        }
    }

    @PostMapping("/evaluate-candidate")
    public ResponseEntity<Map<String, Object>> evaluateCandidateWithText(
            @RequestBody EvaluationRequest requestBody) {

        log.info("Évaluation d'un candidat avec texte reçue");

        try {
            Map<String, Object> result = recruitingAIService.evaluateCandidateMatchWithText(
                    requestBody.getJobDescription(),
                    requestBody.getCandidateCv()
            );
            return ResponseEntity.ok(result);
        } catch (GeminiRateLimitException e) {
            return ResponseEntity.status(429)
                .body(Map.of(
                    "error", "TOO_MANY_REQUESTS",
                    "message", e.getMessage(),
                    "retryAfterSeconds", e.getRetryAfterSeconds()
                ));
        } catch (Exception e) {
            log.error("Erreur lors de l'évaluation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de l'évaluation: " + e.getMessage()));
        }
    }

    @GetMapping("/models")
    public ResponseEntity<Map<String, Object>> listModels() {
        try {
            return ResponseEntity.ok(recruitingAIService.listAvailableModels());
        } catch (Exception e) {
            log.error("Erreur lors du listing des modèles Gemini", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Erreur lors du listing des modèles: " + e.getMessage()));
        }
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getRuntimeConfig() {
        return ResponseEntity.ok(recruitingAIService.getRuntimeGeminiConfig());
    }

    @lombok.Data
    public static class EvaluationRequest {
        private String jobDescription;
        private String candidateCv;
    }

    @lombok.Data
    public static class PdfEvaluationRequest {
        private String jobDescription;
        private String jobDesc;
        // Preferred field name
        private String cvPdfBase64;
        // Backward-compatible alias
        private String cvPdf;
    }
}


