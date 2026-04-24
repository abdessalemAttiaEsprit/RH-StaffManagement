package com.smartpark.backend.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import com.smartpark.backend.dto.SalaryAiRequest;
import com.smartpark.backend.dto.SalaryAiResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "http://localhost:4200")
@Slf4j
public class SalaryAIPrediction {
    @PostMapping("/predict-salary")
    public ResponseEntity<?> getPrediction(@RequestBody SalaryAiRequest request) {
        RestTemplate restTemplate = new RestTemplate();
        String pythonUrl = "http://127.0.0.1:8000/predict-salary";

        // On prépare le payload pour Python
        Map<String, Object> map = new HashMap<>();
        map.put("role_id", request.getRoleId());
        map.put("years_experience", request.getExperience());
        map.put("cv_score_ai", request.getCvScore());
        map.put("contract_type_id", request.getContractTypeId());

        try {
            SalaryAiResponse response = restTemplate.postForObject(pythonUrl, map, SalaryAiResponse.class);
            if (response == null) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(Map.of("error", "AI_SERVICE_UNAVAILABLE", "message", "Réponse vide depuis le service IA"));
            }

            double totalBenefits = response.getTotal_benefits() == null ? 0.0 : response.getTotal_benefits();
            Map<String, Double> avantages = new LinkedHashMap<>();
            avantages.put("primeTransport", round(totalBenefits * 0.40));
            avantages.put("primeRisque", round(totalBenefits * 0.30));
            avantages.put("panier", round(totalBenefits * 0.30));
            response.setAvantages(avantages);

            if (response.getSalary_brut() != null) response.setSalary_brut(round(response.getSalary_brut()));
            if (response.getOvertime_rate() != null) response.setOvertime_rate(round(response.getOvertime_rate()));
            response.setTotal_benefits(round(totalBenefits));

            return ResponseEntity.ok(response);
        } catch (RestClientException ex) {
            log.error("Erreur appel service IA salaire: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of(
                            "error", "AI_SERVICE_UNAVAILABLE",
                            "message", "Impossible de joindre le service IA (FastAPI)"));
        }
    }

    private static double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}

