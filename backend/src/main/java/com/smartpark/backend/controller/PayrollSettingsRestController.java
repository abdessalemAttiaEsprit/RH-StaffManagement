package com.smartpark.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.smartpark.backend.dto.PayrollSettingsDTO;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/payroll/settings")
@CrossOrigin("http://localhost:4200")
public class PayrollSettingsRestController {

    @Value("${payroll.absence.quota-days:0}")
    private long absenceQuotaDays;

    @Value("${payroll.working-days-per-month:22}")
    private int workingDaysPerMonth;

    @GetMapping
    public ResponseEntity<PayrollSettingsDTO> getSettings() {
        return ResponseEntity.ok(
                PayrollSettingsDTO.builder()
                        .absenceQuotaDays(Math.max(0, absenceQuotaDays))
                        .workingDaysPerMonth(Math.max(0, workingDaysPerMonth))
                        .build()
        );
    }
}

