package com.smartpark.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.smartpark.backend.repository.IPaymentRepo;
import com.smartpark.backend.repository.IPersonnelRepo;
import com.smartpark.backend.dto.PaymentDTO;
import com.smartpark.backend.dto.PersonnelDTO;
import com.smartpark.backend.model.Payment;
import com.smartpark.backend.model.Personnel;
import com.smartpark.backend.exceptions.ResourceNotFoundException;
import com.smartpark.backend.mapper.IPaymentMapper;
import com.smartpark.backend.service.IPaymentService;
import com.smartpark.backend.service.IPdfService;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/payments")
@CrossOrigin("http://localhost:4200")
public class PaymentRestController {

    private final IPaymentService paymentService;
    private final IPaymentRepo paymentRepository;
    private final IPaymentMapper paymentMapper;
    private final IPdfService pdfService;
    private final IPersonnelRepo personnelRepository ;

    @PostMapping("/generate-all")
    public ResponseEntity<List<PaymentDTO>> generateAllPayments(@RequestBody Map<String, Integer> payload) {
        int month = payload.get("month");
        int year = payload.get("year");

        List<Personnel> allPersonnel = personnelRepository.findAll();

        List<PaymentDTO> results = allPersonnel.stream()
                .filter(p -> p.getContrat() != null)
                .map(p -> {
                    try {
                        return paymentService.calculateMonthlySalary(p.getMatricule(), month, year);
                    } catch (Exception e) {
                        return null; // Gérer les erreurs individuelles
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return ResponseEntity.ok(results);
    }
    @PutMapping("/matricule/{matricule}")
    public ResponseEntity<PaymentDTO> updatePayment(@PathVariable String matricule, @RequestBody PaymentDTO dto) {
        PaymentDTO updated = paymentService.updatePaymentByDetails(matricule, dto);
        return ResponseEntity.ok(updated);
    }
    @PostMapping("/generate/{matricule}")
    public ResponseEntity<PaymentDTO> createPayment(
            @PathVariable String matricule,
            @RequestParam String month,
            @RequestParam String year) {
        try {

            int monthInt = Integer.parseInt(month.trim().replaceAll("\"", ""));
            int yearInt = Integer.parseInt(year.trim().replaceAll("\"", ""));
            

            if (monthInt < 1 || monthInt > 12) {
                return ResponseEntity.badRequest().body(null);
            }
            
            return ResponseEntity.ok(paymentService.calculateMonthlySalary(matricule, monthInt, yearInt));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        }
    }

    @GetMapping("/generate/{matricule}")
    public ResponseEntity<PaymentDTO> createPaymentGet(
            @PathVariable String matricule,
            @RequestParam String month,
            @RequestParam String year) {
        return createPayment(matricule, month, year);
    }

    @DeleteMapping("/matricule/{matricule}")
    public ResponseEntity<?> deletePayment(@PathVariable String matricule) {
        try {
            paymentService.deleteByMatricule(matricule);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Paiement non trouvé");
        }
    }
    @DeleteMapping("/matricule/{matricule}/{month}/{year}")
    public ResponseEntity<?> deletePaymentForPeriod(
            @PathVariable String matricule,
            @PathVariable int month,
            @PathVariable int year) {
        paymentService.deleteByMatriculeAndMonthAndYear(matricule, month, year);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<PaymentDTO>> getAll() {
        List<Payment> payments = paymentRepository.findAll();
        List<PaymentDTO> dtos = payments.stream()
                .map(paymentMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/pdf/{matricule}/{month}/{year}")
    public ResponseEntity<?> downloadPdf(
            @PathVariable String matricule,
            @PathVariable int month,
            @PathVariable int year) {
        try {
            PaymentDTO p = paymentService.findSpecificPayment(matricule, month, year);
            byte[] pdfBytes = pdfService.generateFichePaie(p);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Fiche_Paie_" + matricule + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(new ByteArrayResource(pdfBytes));
        } catch (ResourceNotFoundException e) {
            System.err.println("[PDF ERROR] Payment not found: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "NOT_FOUND", "message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            System.err.println("[PDF ERROR] Invalid input: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "BAD_REQUEST", "message", e.getMessage()));
        } catch (Exception e) {
            System.err.println("[PDF ERROR] " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "INTERNAL_SERVER_ERROR", "message", e.getMessage()));
        }
    }

}
