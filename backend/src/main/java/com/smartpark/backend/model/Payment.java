package com.smartpark.backend.model;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import com.smartpark.backend.dto.AbsenceDTO;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Document(collection = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Payment {
    @Id
    private String id;
    @Indexed(unique = true)

    private String cin;//cin
    private String matricule;
    private String fullPersonnelName;
    private String rib;
    private String cnssNumber;

    private LocalDate paymentDate;
    private Month month;
    private int year;

    // Snapshot financier
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

    private Double montantCnss;
    private Double montantIrpp;
    private Double finalAmount;

    private String status;
}
