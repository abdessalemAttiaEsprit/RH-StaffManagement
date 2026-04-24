package com.smartpark.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.smartpark.backend.repository.IPaymentRepo;
import com.smartpark.backend.repository.IPersonnelRepo;
import com.smartpark.backend.dto.PaymentDTO;
import com.smartpark.backend.model.Payment;
import com.smartpark.backend.model.Personnel;
import com.smartpark.backend.exceptions.ResourceNotFoundException;
import com.smartpark.backend.mapper.IPaymentMapper; // Assurez-vous du bon package

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Value;

@Service
public class PaymentServiceImpl implements IPaymentService {
    private final IPaymentRepo paymentRepository;
    private final IPaymentMapper paymentMapper;
    private final IPersonnelRepo personnelRepository;
    private final AbsenceQuotaCalculator quotaCalculator;
    private final PaymentEmailNotificationService paymentEmailNotificationService;
    @Autowired
    private IPdfService pdfService;// Vous avez besoin de ceci pour récupérer l'agent


        @Value("${payroll.working-days-per-month:26}")
    private int workingDaysPerMonth;

        @Value("${payroll.calendar-days-per-month:30}")
        private int calendarDaysPerMonth;

    public PaymentServiceImpl(
            IPaymentRepo paymentRepository,
            IPaymentMapper paymentMapper,
            IPersonnelRepo personnelRepository,
            AbsenceQuotaCalculator quotaCalculator,
            PaymentEmailNotificationService paymentEmailNotificationService
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentMapper = paymentMapper;
        this.personnelRepository = personnelRepository;
        this.quotaCalculator = quotaCalculator;
        this.paymentEmailNotificationService = paymentEmailNotificationService;
    }

    @Override
    public PaymentDTO calculateMonthlySalary(String matricule, int month, int year) {
        Personnel p = personnelRepository.findByMatricule(matricule)
                .orElseThrow(() -> new ResourceNotFoundException("Personnel avec le matricule '" + matricule + "' introuvable"));

        if (p.getContrat() == null) {
            throw new IllegalStateException("L'agent avec le matricule '" + matricule + "' n'a pas de contrat actif.");
        }

        java.time.Month monthEnum = java.time.Month.of(month);
        if (paymentRepository.existsByMatriculeAndMonthAndYear(matricule, monthEnum, year)) {
            throw new IllegalStateException("Un paiement existe déjà pour le matricule '" + matricule + "' pour " + monthEnum + " " + year);
        }

        double salaireBrutMensuel = p.getContrat().getSalaireBase();

        // ===== PRORATISATION BASÉE SUR LE CONTRAT (DATE DÉBUT / DATE FIN) =====
        //  on paie uniquement la portion du mois couverte par le contrat.
        // Exemple: Avril, contrat débute le 14 => jours payés = 26 - 13.
        // Exemple: Mai, contrat finit le 18 => jours payés = 26 - 12.
        YearMonth payrollPeriod = YearMonth.of(year, month);

        if (workingDaysPerMonth <= 0) {
            throw new IllegalStateException("Configuration invalide: payroll.working-days-per-month doit être > 0");
        }
        if (calendarDaysPerMonth <= 0) {
            throw new IllegalStateException("Configuration invalide: payroll.calendar-days-per-month doit être > 0");
        }
        if (calendarDaysPerMonth < workingDaysPerMonth) {
            throw new IllegalStateException("Configuration invalide: payroll.calendar-days-per-month doit être >= payroll.working-days-per-month");
        }

        LocalDate periodStart = payrollPeriod.atDay(1);
        LocalDate periodEnd = payrollPeriod.atEndOfMonth();

        LocalDate contractStart = p.getContrat().getDateDebut();
        LocalDate contractEnd = p.getContrat().getDateFin();
        if (contractStart == null) {
            throw new IllegalStateException("Le contrat de l'agent '" + matricule + "' n'a pas de dateDebut.");
        }

        LocalDate activeStart = contractStart.isAfter(periodStart) ? contractStart : periodStart;
        LocalDate activeEnd = periodEnd;
        if (contractEnd != null && contractEnd.isBefore(activeEnd)) {
            activeEnd = contractEnd;
        }

        if (activeEnd.isBefore(activeStart)) {
            throw new IllegalStateException(
                    "Contrat inactif sur la période demandée (" + payrollPeriod + ") pour le matricule '" + matricule + "'."
            );
        }

        int startDayAssumed = Math.min(activeStart.getDayOfMonth(), calendarDaysPerMonth);
        int endDayAssumed = Math.min(activeEnd.getDayOfMonth(), calendarDaysPerMonth);
        long calendarDaysWorkedAssumed = (endDayAssumed >= startDayAssumed)
                ? (long) (endDayAssumed - startDayAssumed + 1)
                : 0L;

        int nonPaidDaysPerMonth = Math.max(0, calendarDaysPerMonth - workingDaysPerMonth);
        long payableDays = Math.max(0L, calendarDaysWorkedAssumed - nonPaidDaysPerMonth);
        payableDays = Math.min(payableDays, (long) workingDaysPerMonth);

        double dailyRate = salaireBrutMensuel / (double) workingDaysPerMonth;
        double salaireBrut = dailyRate * (double) payableDays;
        double prorationRatio = Math.min(1.0, (double) payableDays / (double) workingDaysPerMonth);
        
        // ===== AVANTAGES =====
        double avantagesTotal = 0;
        java.util.Map<String, Double> avantagesMap = new java.util.HashMap<>();
        if (p.getContrat().getAvantages() != null) {
            if (p.getContrat().getAvantages().containsKey("primeTransport")) {
                double primeTransport = (double) p.getContrat().getAvantages().getOrDefault("primeTransport", 0.0);
                // Proratisation des avantages mensuels sur la même période
                primeTransport = primeTransport * prorationRatio;
                avantagesMap.put("primeTransport", primeTransport);
                avantagesTotal += primeTransport;
            }
            if (p.getContrat().getAvantages().containsKey("primeRisque")) {
                double primeRisque = (double) p.getContrat().getAvantages().getOrDefault("primeRisque", 0.0);
                primeRisque = primeRisque * prorationRatio;
                avantagesMap.put("primeRisque", primeRisque);
                avantagesTotal += primeRisque;
            }
            if (p.getContrat().getAvantages().containsKey("panier")) {
                double panier = (double) p.getContrat().getAvantages().getOrDefault("panier", 0.0);
                panier = panier * prorationRatio;
                avantagesMap.put("panier", panier);
                avantagesTotal += panier;
            }
        }

        // ===== ABSENCES (PÉRIODE DU MOIS DE PAIE) =====
        // Nouveau besoin: la paie du mois M doit déduire les absences du même mois M, sur tout le mois.
        // Donc on ne borne pas la période à aujourd'hui (les absences futures dans le mois sont prises en compte).
        LocalDate absencePeriodStart = periodStart;
        LocalDate absencePeriodEnd = periodEnd;

        long totalAbsenceDaysReference = 0;
        long nonJustifiedAbsenceDaysReference = 0;
        long justifiedAbsenceDaysReference = 0;
        if (p.getAbsences() != null && !p.getAbsences().isEmpty()) {
            // Total (tous statuts) sur la période du mois de paie
            totalAbsenceDaysReference = p.getAbsences().stream()
                    .filter(a -> a != null && a.getStartDate() != null && a.getEndDate() != null)
                    .filter(a -> !a.getEndDate().isBefore(absencePeriodStart) && !a.getStartDate().isAfter(absencePeriodEnd))
                    .mapToLong(a -> {
                        LocalDate overlapStart = a.getStartDate().isBefore(absencePeriodStart) ? absencePeriodStart : a.getStartDate();
                        LocalDate overlapEnd = a.getEndDate().isAfter(absencePeriodEnd) ? absencePeriodEnd : a.getEndDate();
                        if (overlapEnd.isBefore(overlapStart)) return 0;
                        return ChronoUnit.DAYS.between(overlapStart, overlapEnd) + 1;
                    })
                    .sum();

            // Non justifiées: PENDING / REJECTED (et compat UNJUSTIFIED ou vide)
            nonJustifiedAbsenceDaysReference = p.getAbsences().stream()
                    .filter(a -> a != null && a.getStartDate() != null && a.getEndDate() != null)
                    .filter(a -> !a.getEndDate().isBefore(absencePeriodStart) && !a.getStartDate().isAfter(absencePeriodEnd))
                    .filter(a -> {
                        String status = a.getStatus();
                        if (status == null || status.isBlank()) return true;
                        return AbsenceQuotaCalculator.isNonJustifiedStatus(status);
                    })
                    .mapToLong(a -> {
                        LocalDate overlapStart = a.getStartDate().isBefore(absencePeriodStart) ? absencePeriodStart : a.getStartDate();
                        LocalDate overlapEnd = a.getEndDate().isAfter(absencePeriodEnd) ? absencePeriodEnd : a.getEndDate();
                        if (overlapEnd.isBefore(overlapStart)) return 0;
                        return ChronoUnit.DAYS.between(overlapStart, overlapEnd) + 1;
                    })
                    .sum();

            // Justifiées: JUSTIFIED / ACCEPTED / APPROVED / VALIDATED
            justifiedAbsenceDaysReference = p.getAbsences().stream()
                    .filter(a -> a != null && a.getStartDate() != null && a.getEndDate() != null)
                    .filter(a -> !a.getEndDate().isBefore(absencePeriodStart) && !a.getStartDate().isAfter(absencePeriodEnd))
                    .filter(a -> AbsenceQuotaCalculator.isJustifiedStatus(a.getStatus()))
                    .mapToLong(a -> {
                        LocalDate overlapStart = a.getStartDate().isBefore(absencePeriodStart) ? absencePeriodStart : a.getStartDate();
                        LocalDate overlapEnd = a.getEndDate().isAfter(absencePeriodEnd) ? absencePeriodEnd : a.getEndDate();
                        if (overlapEnd.isBefore(overlapStart)) return 0;
                        return ChronoUnit.DAYS.between(overlapStart, overlapEnd) + 1;
                    })
                    .sum();
        }

        // ===== QUOTA ABSENCE CUMULATIF (2 JOURS / MOIS) =====
        // Règle: les absences non justifiées sont toujours pénalisées.
        // Les absences justifiées consomment le quota (cumulé depuis le début du contrat).
        long quotaMonthly = quotaCalculator.getMonthlyQuotaDays();
        long quotaAvailableBefore = quotaCalculator.computeAvailableQuotaBeforePeriod(p, absencePeriodStart, absencePeriodEnd);
        long quotaUsedJustified = Math.min(quotaAvailableBefore, Math.max(0, justifiedAbsenceDaysReference));
        long penalizedJustifiedOverflow = Math.max(0, justifiedAbsenceDaysReference - quotaAvailableBefore);
        long quotaRemainingAfter = Math.max(0, quotaAvailableBefore - quotaUsedJustified);

        long penalizedNonJustified = Math.max(0, nonJustifiedAbsenceDaysReference);
        long penalizedAbsenceDays = penalizedNonJustified + penalizedJustifiedOverflow;
        double deductionAbsence = dailyRate * penalizedAbsenceDays;
        
        // ===== CALCUL SALAIRE BRUT COTISABLE =====
        double salaireBrutCotisable = salaireBrut + avantagesTotal;
        
        // ===== COTISATIONS =====
        double montantCnss = salaireBrutCotisable * 0.0918;
        double baseImposable = salaireBrutCotisable - deductionAbsence;
        double montantIrpp = (baseImposable < 600) ? (baseImposable * 0.05) : (baseImposable * 0.10);
        
        // ===== NET À PAYER =====
        double finalAmount = salaireBrut + avantagesTotal - deductionAbsence - montantCnss - montantIrpp;

        Payment payment = Payment.builder()
                .cin(p.getCin())
                .fullPersonnelName(p.getNom() + " " + p.getPrenom())
                .matricule(p.getMatricule())
                .rib(p.getRib())
                .cnssNumber(p.getCnssNumber())
                .paymentDate(LocalDate.now())
                .month(Month.of(month))
                .year(year)
                .salaireBase(salaireBrut)
                .salaireBaseMensuel(salaireBrutMensuel)
                .workingDaysPerMonth(workingDaysPerMonth)
                .payableDays(payableDays)
                .avantages(avantagesMap)
                // Pour affichage (y compris JUSTIFIED)
                .referenceAbsenceDays(totalAbsenceDaysReference)
                // On affiche les jours réellement déduits dans le bulletin
                .totalAbsenceDays(penalizedAbsenceDays)
                .deductionsAbsence(deductionAbsence)

                .absencePeriodStart(absencePeriodStart)
                .absencePeriodEnd(absencePeriodEnd)
                .absenceQuotaMonthlyDays(quotaMonthly)
                .absenceQuotaEarnedDays(quotaCalculator.computeAsOf(p, absencePeriodEnd).earnedDays())
                .absenceQuotaAvailableBefore(quotaAvailableBefore)
                .absenceQuotaUsedJustified(quotaUsedJustified)
                .absenceQuotaRemainingAfter(quotaRemainingAfter)
                .justifiedAbsenceDays(justifiedAbsenceDaysReference)
                .nonJustifiedAbsenceDays(nonJustifiedAbsenceDaysReference)
                .penalizedJustifiedAbsenceDays(penalizedJustifiedOverflow)
                .penalizedNonJustifiedAbsenceDays(penalizedNonJustified)

                .montantCnss(montantCnss)
                .montantIrpp(montantIrpp)
                .finalAmount(finalAmount)
                .status(finalAmount < 0 ? "DISCIPLINE_COUNCIL" : "PENDING")
                .build();

        return paymentMapper.toDto(paymentRepository.save(payment));
    }

    @Override
    public PaymentDTO findByMatricule(String matricule) {
        // Vérification de sécurité optionnelle
        if (matricule == null || matricule.trim().isEmpty()) {
            throw new IllegalArgumentException("Le matricule fourni ne peut pas être vide.");
        }

        return paymentRepository.findByMatricule(matricule)
                .map(paymentMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Aucun paiement trouvé pour le matricule : " + matricule));
    }

    // Optionnel : Une méthode pour récupérer directement les octets du PDF
    public byte[] getPaymentPdf(String matricule) {
        PaymentDTO dto = this.findByMatricule(matricule);
        // On appelle le service de création de PDF (voir code précédent)
        return pdfService.generateFichePaie(dto);
    }

    public void deleteByMatricule(String matricule) {
        paymentRepository.deleteByMatricule(matricule);
    }

    @Override
    public void deleteByMatriculeAndMonthAndYear(String matricule, int month, int year) {
        if (matricule == null || matricule.trim().isEmpty()) {
            throw new IllegalArgumentException("Le matricule fourni ne peut pas être vide.");
        }
        Month monthEnum;
        try {
            monthEnum = Month.of(month);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Mois invalide : " + month);
        }

        Payment existing = paymentRepository.findByMatriculeAndMonthAndYear(matricule, monthEnum, year)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Paiement introuvable pour le matricule %s (Période : %02d/%d)", matricule, month, year)
                ));

        paymentRepository.delete(existing);
    }

    @Override
    public PaymentDTO findSpecificPayment(String matricule, int month, int year) {
        // 1. Validation de sécurité de base
        if (matricule == null || matricule.isEmpty()) {
            throw new IllegalArgumentException("Le matricule ne peut pas être nul ou vide.");
        }

        // 2. Conversion de l'int month en Enum Month (1 -> JANUARY, etc.)
        Month monthEnum;
        try {
            monthEnum = Month.of(month);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Mois invalide : " + month);
        }

        // 3. Recherche dans le repository
        // Note : On suppose que vous avez cette signature dans IPaymentRepo
        return paymentRepository.findByMatriculeAndMonthAndYear(matricule, monthEnum, year)
                .map(paymentMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Aucun bulletin de paie trouvé pour le matricule %s (Période : %02d/%d)",
                                matricule, month, year)
                ));
    }

    // --- Modification ---
    @Override
    public PaymentDTO updatePaymentByDetails(String matricule, PaymentDTO paymentDTO) {
        // 1. Extraction du mois depuis le DTO (String -> Enum)
        // Correction : on utilise paymentDTO (le nom du paramètre)
        Month monthEnum = paymentDTO.getMonth();

        // 2. Recherche du paiement
        // On récupère l'année via paymentDTO.getYear()
        Payment existing = paymentRepository.findByMatriculeAndMonthAndYear(
                matricule,
                monthEnum,
                paymentDTO.getYear()
        ).orElseThrow(() -> new ResourceNotFoundException("Paiement introuvable pour le matricule '" + matricule + "' et la période " + paymentDTO.getMonth() + "/" + paymentDTO.getYear()));

        String previousStatus = existing.getStatus();

        // 3. Mise à jour des champs (on utilise paymentDTO pour les nouvelles valeurs)
        existing.setRib(paymentDTO.getRib());

        Double dtoFinalAmount = paymentDTO.getFinalAmount();
        existing.setFinalAmount(dtoFinalAmount);

        // RÈGLE MÉTIER : si salaire/net à payer est négatif => conseil de discipline
        if (dtoFinalAmount != null && dtoFinalAmount < 0) {
            existing.setStatus("DISCIPLINE_COUNCIL");
        } else {
            existing.setStatus(paymentDTO.getStatus());
        }

        // 4. Sauvegarde et retour
        Payment saved = paymentRepository.save(existing);

        // Notification email uniquement lors du passage PENDING -> PAID
        boolean wasPaid = previousStatus != null && "PAID".equalsIgnoreCase(previousStatus);
        boolean isPaid = saved.getStatus() != null && "PAID".equalsIgnoreCase(saved.getStatus());
        if (!wasPaid && isPaid) {
            personnelRepository.findByMatricule(matricule)
                    .ifPresent(personnel -> paymentEmailNotificationService.notifyPaymentValidated(personnel, saved));
        }
        return paymentMapper.toDto(saved);
    }


}


