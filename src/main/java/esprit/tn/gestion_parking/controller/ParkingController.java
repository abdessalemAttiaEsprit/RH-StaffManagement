package esprit.tn.gestion_parking.controller;

import esprit.tn.gestion_parking.dto.ParkingDTO;
import esprit.tn.gestion_parking.service.IParkingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parkings")
@CrossOrigin(origins = "*" , allowedHeaders = "*")
public class ParkingController {

    @Autowired
    private IParkingService parkingService;

    @GetMapping
    public ResponseEntity<List<ParkingDTO>> getAllParkings() {
        return ResponseEntity.ok(parkingService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ParkingDTO> getParkingById(@PathVariable String id) {
        ParkingDTO parking = parkingService.findById(id);
        return (parking != null) ? ResponseEntity.ok(parking) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<ParkingDTO> createParking(@RequestBody ParkingDTO parkingDTO) {
        return new ResponseEntity<>(parkingService.addParking(parkingDTO), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ParkingDTO> updateParking(@PathVariable String id, @RequestBody ParkingDTO parkingDTO) {
        ParkingDTO updated = parkingService.updateParking(id, parkingDTO);
        return (updated != null) ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteParking(@PathVariable String id) {
        parkingService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/promotions")
    public ResponseEntity<List<ParkingDTO>> getPromotions() {
        return ResponseEntity.ok(parkingService.findActivePromotions());
    }
}