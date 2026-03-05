package tn.esprit.sallesmateriels.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import tn.esprit.sallesmateriels.entities.Materiel;
import tn.esprit.sallesmateriels.entities.MaterielStatus;
import tn.esprit.sallesmateriels.entities.Salle;
import tn.esprit.sallesmateriels.repositories.MaterielRepository;
import tn.esprit.sallesmateriels.repositories.SalleRepository;

import java.util.List;

@RestController
@RequestMapping("/api/materiels")
@CrossOrigin(origins = "*")
public class MaterielController {

    @Autowired
    private MaterielRepository materielRepository;
    @Autowired
    private SalleRepository salleRepository;

    @GetMapping
    public ResponseEntity<List<Materiel>> getAll() {
        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl("no-store, no-cache, must-revalidate");
        return ResponseEntity.ok().headers(headers).body(materielRepository.findAll());
    }

    @GetMapping("/stats")
    public ResponseEntity<java.util.Map<String, Object>> getStats() {
        long total = materielRepository.countTotal();
        long available = materielRepository.countByStatus(MaterielStatus.AVAILABLE);
        return ResponseEntity.ok(java.util.Map.of(
                "totalMateriels", total,
                "availableMateriels", available
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Materiel> getById(@PathVariable("id") Integer id) {
        return materielRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Materiel> create(
            @RequestBody java.util.Map<String, Object> body,
            @RequestParam(value = "salleId", required = false) Integer salleId) {
        String nom = body != null && body.get("nom") != null ? body.get("nom").toString().trim() : null;
        String statusStr = body != null && body.get("status") != null ? body.get("status").toString() : null;
        if (nom == null || nom.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        MaterielStatus status = toStatus(statusStr);
        if (status == null) status = MaterielStatus.AVAILABLE;

        Materiel m = new Materiel();
        m.setNom(nom);
        m.setStatus(status);
        if (salleId != null && salleId > 0) {
            salleRepository.findById(salleId).ifPresent(m::setSalle);
        }
        return ResponseEntity.ok(materielRepository.save(m));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Materiel> update(
            @PathVariable("id") Integer id,
            @RequestBody java.util.Map<String, Object> body,
            @RequestParam(value = "salleId", required = false) Integer salleId) {
        return materielRepository.findById(id)
                .map(m -> {
                    if (body != null) {
                        if (body.get("nom") != null) m.setNom(body.get("nom").toString().trim());
                        MaterielStatus s = toStatus(body.get("status") != null ? body.get("status").toString() : null);
                        if (s != null) m.setStatus(s);
                    }
                    if (salleId != null) {
                        if (salleId <= 0) m.setSalle(null);
                        else salleRepository.findById(salleId).ifPresent(m::setSalle);
                    }
                    return ResponseEntity.ok(materielRepository.save(m));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> delete(@PathVariable("id") Integer id) {
        if (!materielRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        materielRepository.deleteByIdNative(id);
        return ResponseEntity.ok(java.util.Map.of("deleted", true));
    }

    private static MaterielStatus toStatus(String s) {
        if (s == null) return null;
        try {
            return MaterielStatus.valueOf(s.toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }
}
