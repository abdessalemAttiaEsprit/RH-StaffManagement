package com.smartpark.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollSettingsDTO {
    private long absenceQuotaDays;
    private int workingDaysPerMonth;
}

