package com.smartpark.backend.dto;

import lombok.*;

import java.util.Map;

@Getter
@Setter
public class SalaryAiResponse {
    private Double salary_brut;
    private Double overtime_rate;
    private Double total_benefits;
    private Map<String, Double> avantages;
}

