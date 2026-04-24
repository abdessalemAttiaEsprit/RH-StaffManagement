package com.smartpark.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.smartpark.backend.model.Absence;
import com.smartpark.backend.model.Personnel;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class AbsenceQuotaCalculator {
    @Value("${payroll.absence.quota-days:2}")
    private long monthlyQuotaDays;

    public record QuotaSnapshot(
            long monthlyQuotaDays,
            long earnedDays,
            long usedJustifiedDays,
            long remainingDays,
            LocalDate asOfDate
    ) {}

    public QuotaSnapshot computeAsOf(Personnel personnel, LocalDate asOfDate) {
        if (personnel == null) {
            throw new IllegalArgumentException("Personnel ne peut pas être null");
        }
        if (asOfDate == null) {
            throw new IllegalArgumentException("asOfDate ne peut pas être null");
        }
        if (personnel.getContrat() == null || personnel.getContrat().getDateDebut() == null) {
            return new QuotaSnapshot(Math.max(0, monthlyQuotaDays), 0, 0, 0, asOfDate);
        }

        LocalDate contractStart = personnel.getContrat().getDateDebut();
        if (asOfDate.isBefore(contractStart)) {
            return new QuotaSnapshot(Math.max(0, monthlyQuotaDays), 0, 0, 0, asOfDate);
        }

        YearMonth startYm = YearMonth.from(contractStart);
        YearMonth endYm = YearMonth.from(asOfDate);
        long monthsEarned = monthsBetweenInclusive(startYm, endYm);
        long earned = Math.max(0, monthsEarned) * Math.max(0, monthlyQuotaDays);

        long usedJustified = countJustifiedAbsenceDays(personnel.getAbsences(), contractStart, asOfDate);
        long remaining = Math.max(0, earned - Math.max(0, usedJustified));

        return new QuotaSnapshot(Math.max(0, monthlyQuotaDays), earned, usedJustified, remaining, asOfDate);
    }

    public long computeAvailableQuotaBeforePeriod(Personnel personnel, LocalDate periodStart, LocalDate periodEnd) {
        if (personnel == null) {
            throw new IllegalArgumentException("Personnel ne peut pas être null");
        }
        if (periodStart == null || periodEnd == null) {
            throw new IllegalArgumentException("Période de référence invalide");
        }
        if (periodEnd.isBefore(periodStart)) {
            throw new IllegalArgumentException("periodEnd avant periodStart");
        }
        if (personnel.getContrat() == null || personnel.getContrat().getDateDebut() == null) {
            return 0;
        }

        LocalDate contractStart = personnel.getContrat().getDateDebut();
        if (periodEnd.isBefore(contractStart)) {
            return 0;
        }

        YearMonth startYm = YearMonth.from(contractStart);
        YearMonth refYm = YearMonth.from(periodEnd);

        long monthsEarned = monthsBetweenInclusive(startYm, refYm);
        long earned = Math.max(0, monthsEarned) * Math.max(0, monthlyQuotaDays);

        LocalDate before = periodStart.minusDays(1);
        if (before.isBefore(contractStart)) {
            before = contractStart.minusDays(1);
        }

        long usedJustifiedBefore = 0;
        if (!before.isBefore(contractStart)) {
            usedJustifiedBefore = countJustifiedAbsenceDays(personnel.getAbsences(), contractStart, before);
        }

        return Math.max(0, earned - usedJustifiedBefore);
    }

    public long getMonthlyQuotaDays() {
        return Math.max(0, monthlyQuotaDays);
    }

    public static boolean isJustifiedStatus(String status) {
        String s = status != null ? status.trim().toUpperCase() : "";
        // Nouveau besoin: PENDING est traité comme justifié
        return "PENDING".equals(s)
            || "JUSTIFIED".equals(s)
                || "ACCEPTED".equals(s)
                || "APPROVED".equals(s)
                || "VALIDATED".equals(s);
    }

    public static boolean isNonJustifiedStatus(String status) {
        String s = status != null ? status.trim().toUpperCase() : "";
        if (s.isBlank()) return true;
        return "REJECTED".equals(s)
                || "UNJUSTIFIED".equals(s);
    }

    private static long monthsBetweenInclusive(YearMonth start, YearMonth end) {
        if (start == null || end == null) return 0;
        if (end.isBefore(start)) return 0;
        return ChronoUnit.MONTHS.between(start, end) + 1;
    }

    public static long countJustifiedAbsenceDays(List<Absence> absences, LocalDate rangeStart, LocalDate rangeEnd) {
        if (absences == null || absences.isEmpty()) return 0;
        if (rangeStart == null || rangeEnd == null) return 0;
        if (rangeEnd.isBefore(rangeStart)) return 0;

        return absences.stream()
                .filter(a -> a != null && a.getStartDate() != null && a.getEndDate() != null)
                .filter(a -> !a.getEndDate().isBefore(rangeStart) && !a.getStartDate().isAfter(rangeEnd))
                .filter(a -> isJustifiedStatus(a.getStatus()))
                .mapToLong(a -> overlapDaysInclusive(a.getStartDate(), a.getEndDate(), rangeStart, rangeEnd))
                .sum();
    }

    public static long countAbsenceDaysByPredicate(List<Absence> absences, LocalDate rangeStart, LocalDate rangeEnd, java.util.function.Predicate<Absence> predicate) {
        if (absences == null || absences.isEmpty()) return 0;
        if (rangeStart == null || rangeEnd == null) return 0;
        if (rangeEnd.isBefore(rangeStart)) return 0;

        return absences.stream()
                .filter(a -> a != null && a.getStartDate() != null && a.getEndDate() != null)
                .filter(a -> !a.getEndDate().isBefore(rangeStart) && !a.getStartDate().isAfter(rangeEnd))
                .filter(predicate)
                .mapToLong(a -> overlapDaysInclusive(a.getStartDate(), a.getEndDate(), rangeStart, rangeEnd))
                .sum();
    }

    private static long overlapDaysInclusive(LocalDate aStart, LocalDate aEnd, LocalDate rangeStart, LocalDate rangeEnd) {
        LocalDate overlapStart = aStart.isBefore(rangeStart) ? rangeStart : aStart;
        LocalDate overlapEnd = aEnd.isAfter(rangeEnd) ? rangeEnd : aEnd;
        if (overlapEnd.isBefore(overlapStart)) return 0;
        return ChronoUnit.DAYS.between(overlapStart, overlapEnd) + 1;
    }
}

