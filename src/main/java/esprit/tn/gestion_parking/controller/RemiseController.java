package esprit.tn.gestion_parking.controller;

import esprit.tn.gestion_parking.dto.RemiseDTO;
import esprit.tn.gestion_parking.service.IRemiseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/remises")
@CrossOrigin(origins = "*")
public class RemiseController {

    @Autowired
    private IRemiseService remiseService;

    @GetMapping("/parking/{parkingId}")
    public ResponseEntity<List<RemiseDTO>> getByParking(@PathVariable String parkingId) {
        return ResponseEntity.ok(remiseService.getByParking(parkingId));
    }

    @PostMapping
    public ResponseEntity<RemiseDTO> create(@RequestBody RemiseDTO remiseDTO) {
        return new ResponseEntity<>(remiseService.addRemise(remiseDTO), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RemiseDTO> update(@PathVariable String id, @RequestBody RemiseDTO remiseDTO) {
        RemiseDTO updated = remiseService.updateRemise(id, remiseDTO);
        return (updated != null) ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        remiseService.deleteRemise(id);
        return ResponseEntity.noContent().build();
    }
}