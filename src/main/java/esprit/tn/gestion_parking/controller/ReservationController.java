package esprit.tn.gestion_parking.controller;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import esprit.tn.gestion_parking.dto.ReservationDTO;
import esprit.tn.gestion_parking.service.IReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservations")
@CrossOrigin(origins = "*",allowedHeaders = "*")
public class ReservationController {

    @Autowired
    private IReservationService reservationService;

    @GetMapping
    public ResponseEntity<List<ReservationDTO>> getAll() {
        return ResponseEntity.ok(reservationService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationDTO> getById(@PathVariable String id) {
        ReservationDTO res = reservationService.getById(id);
        return (res != null) ? ResponseEntity.ok(res) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ReservationDTO reservationDTO) {
        try {
            return new ResponseEntity<>(reservationService.createReservation(reservationDTO), HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable String id) {
        reservationService.cancelReservation(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/spot/{spotId}")
    public ResponseEntity<List<ReservationDTO>> getBySpot(@PathVariable String spotId) {
        return ResponseEntity.ok(reservationService.findBySpot(spotId));
    }

    @GetMapping("/vehicle/{matricule}")
    public ResponseEntity<List<ReservationDTO>> getByVehicle(@PathVariable String matricule) {
        return ResponseEntity.ok(reservationService.findByVehicle(matricule));
    }

    @GetMapping("/parking/{parkingId}")
    public ResponseEntity<List<ReservationDTO>> getByParking(@PathVariable String parkingId) {
        return ResponseEntity.ok(reservationService.findByParking(parkingId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateReservation(@PathVariable String id, @RequestBody ReservationDTO updatedReservation) {
        try {
            return ResponseEntity.ok(reservationService.updateReservation(id, updatedReservation));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/valider-flux")
    public ResponseEntity<ReservationDTO> validerFlux(@PathVariable String id, @RequestBody Map<String, Object> updates) {
        ReservationDTO res = reservationService.getById(id);
        if (res == null) return ResponseEntity.notFound().build();

        if (updates.containsKey("statusAction")) {
            res.setStatusAction((String) updates.get("statusAction"));
        }
        if (updates.containsKey("montantFinal")) {
            res.setMontantFinal(Double.valueOf(updates.get("montantFinal").toString()));
        }

        try {
            return ResponseEntity.ok(reservationService.updateReservation(id, res));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/pdf/{id}")
    public ResponseEntity<byte[]> generateTicketPdf(@PathVariable String id) {
        ReservationDTO res = reservationService.getById(id);
        if (res == null) return ResponseEntity.notFound().build();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A6);
            PdfWriter.getInstance(document, out);
            document.open();

            Font boldFont = new Font(Font.HELVETICA, 14, Font.BOLD);
            Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL);

            document.add(new Paragraph("RECU PARKING", boldFont));
            document.add(new Paragraph("------------------------------", normalFont));
            document.add(new Paragraph("ID: " + res.getId(), normalFont));
            document.add(new Paragraph("MATRICULE: " + res.getMatricule(), normalFont));
            document.add(new Paragraph("ENTREE: " + res.getDatetimeEntree(), normalFont));
            document.add(new Paragraph("SORTIE: " + res.getDatetimeSortie(), normalFont));
            document.add(new Paragraph("------------------------------", normalFont));
            document.add(new Paragraph("PRIX: " + res.getMontant() + " TND", boldFont));
            document.close();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("inline", "ticket.pdf");

            return new ResponseEntity<>(out.toByteArray(), headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/entree-ia")
    public ResponseEntity<ReservationDTO> entreeIA(@RequestBody ReservationDTO dto) {
        return ResponseEntity.ok(reservationService.enregistrerPassageAuto(dto));
    }
}