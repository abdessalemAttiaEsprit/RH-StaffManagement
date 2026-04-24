package com.smartpark.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import com.smartpark.backend.model.Payment;
import com.smartpark.backend.model.Personnel;

import java.time.Month;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEmailNotificationService {

    private final JavaMailSender mailSender;

    @Value("${notifications.mail.enabled:true}")
    private boolean mailEnabled;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    @Value("${spring.mail.host:}")
    private String mailHost;

    public void notifyPaymentValidated(Personnel personnel, Payment payment) {
        if (!mailEnabled) {
            return;
        }
        if (personnel == null || payment == null) {
            return;
        }
        if (mailHost == null || mailHost.isBlank()) {
            log.warn("Mail not configured (spring.mail.host missing). Skipping payment validation email.");
            return;
        }

        String to = personnel.getEmail();
        if (to == null || to.isBlank()) {
            log.warn("Personnel email missing for matricule {}. Skipping payment validation email.", personnel.getMatricule());
            return;
        }

        String fullName = (personnel.getPrenom() != null ? personnel.getPrenom() : "")
                + (personnel.getNom() != null ? (" " + personnel.getNom()) : "");
        fullName = fullName.trim();
        if (fullName.isBlank()) {
            fullName = personnel.getMatricule();
        }

        Month month = payment.getMonth();
        String periodLabel = (month != null ? month.toString() : "") + " " + payment.getYear();

        String subject = "Paiement validé - " + periodLabel;
        String net = payment.getFinalAmount() != null ? String.format("%.3f", payment.getFinalAmount()) : "0";

        String body = "Bonjour " + fullName + ",\n\n"
                + "Votre paiement  pour la période " + periodLabel + " a été validé avec succes." + "\n"
                + "Salaire net  : " + net + " TND\n\n"
                + "Vous pouvez contacter responsable RH pour votre bulletin de paie .\n"
                + "Cordialement,\n"
                + "SmartPark RH";

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (fromAddress != null && !fromAddress.isBlank()) {
                message.setFrom(fromAddress);
            }
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.info("Payment validation email sent to {} for matricule {} ({})", to, personnel.getMatricule(), periodLabel);
        } catch (Exception e) {
            // On ne bloque pas la validation du paiement si l'email échoue.
            log.warn("Failed to send payment validation email to {} (matricule {}). Cause: {}",
                    to, personnel.getMatricule(), e.getMessage());
        }
    }
}

