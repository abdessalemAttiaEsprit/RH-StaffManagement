package com.smartpark.backend.dto;

import lombok.*;
import org.springframework.data.annotation.Id;

import java.time.LocalDate;
import java.time.Month;
import java.util.Map;
@Data // GÉNÈRE GETTERS/SETTERS
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PaymentDTO {
    private String id;
    private String cin;
    private String matricule;
    private String fullPersonnelName;
    private String rib;
    private String cnssNumber;
    private LocalDate paymentDate;
    private Month month;
    private int year;
    private Double salaireBase;
    private Double salaireBaseMensuel;
    private Integer workingDaysPerMonth;
    private Long payableDays;
    private Double tauxHoraireSup;
    private Map<String, Double> avantages;
    private long referenceAbsenceDays;
    private long totalAbsenceDays;
    private Double deductionsAbsence;
    private LocalDate absencePeriodStart;
    private LocalDate absencePeriodEnd;
    private Long absenceQuotaMonthlyDays;
    private Long absenceQuotaEarnedDays;
    private Long absenceQuotaAvailableBefore;
    private Long absenceQuotaUsedJustified;
    private Long absenceQuotaRemainingAfter;
    private Long justifiedAbsenceDays;
    private Long nonJustifiedAbsenceDays;
    private Long penalizedJustifiedAbsenceDays;
    private Long penalizedNonJustifiedAbsenceDays;

    private Double montantCnss; // Le montant calculé (Salaire * 9.18%)
    private Double montantIrpp; // L'impôt calculé
    private Double finalAmount; // Net à payer

    private String status;

    // Getters explicites (aide certains analyseurs/IDE quand Lombok est mal indexé)
    public Double getSalaireBaseMensuel() { return salaireBaseMensuel; }
    public Integer getWorkingDaysPerMonth() { return workingDaysPerMonth; }
    public Long getPayableDays() { return payableDays; }

    public LocalDate getAbsencePeriodStart() { return absencePeriodStart; }
    public LocalDate getAbsencePeriodEnd() { return absencePeriodEnd; }

    public Long getAbsenceQuotaMonthlyDays() { return absenceQuotaMonthlyDays; }
    public Long getAbsenceQuotaAvailableBefore() { return absenceQuotaAvailableBefore; }
    public Long getAbsenceQuotaRemainingAfter() { return absenceQuotaRemainingAfter; }

    public Long getJustifiedAbsenceDays() { return justifiedAbsenceDays; }
    public Long getNonJustifiedAbsenceDays() { return nonJustifiedAbsenceDays; }
    public Long getPenalizedJustifiedAbsenceDays() { return penalizedJustifiedAbsenceDays; }
}

