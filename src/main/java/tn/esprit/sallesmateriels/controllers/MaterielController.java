package tn.esprit.sallesmateriels.controllers;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.sallesmateriels.entities.Materiel;
import tn.esprit.sallesmateriels.service.MaterielService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/materiels")
public class MaterielController {

    private final MaterielService materielService;

    public MaterielController(MaterielService materielService) {
        this.materielService = materielService;
    }

    @GetMapping
    public ResponseEntity<List<Materiel>> getAll() {
        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl("no-store, no-cache, must-revalidate");
        return ResponseEntity.ok().headers(headers).body(materielService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Materiel> getById(@PathVariable("id") Integer id) {
        return materielService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(
            @RequestBody Map<String, Object> body,
            @RequestParam(value = "salleId", required = false) Integer salleId) {
        MaterielService.MaterielSaveOutcome outcome = materielService.create(body, salleId);
        if (outcome.getKind() == MaterielService.MaterielSaveOutcome.Kind.BAD_REQUEST) {
            if (outcome.getErrorMessage() != null) {
                return ResponseEntity.badRequest().body(Map.of("message", outcome.getErrorMessage()));
            }
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(Map.of("materiel", outcome.getMateriel(), "warnings", outcome.getWarnings()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable("id") Integer id,
            @RequestBody Map<String, Object> body,
            @RequestParam(value = "salleId", required = false) Integer salleId) {
        return materielService.update(id, body, salleId)
                .map(outcome -> {
                    if (outcome.getKind() == MaterielService.MaterielSaveOutcome.Kind.BAD_REQUEST) {
                        return ResponseEntity.badRequest().body(Map.of("message", outcome.getErrorMessage()));
                    }
                    return ResponseEntity.ok(Map.of("materiel", outcome.getMateriel(), "warnings", outcome.getWarnings()));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") Integer id) {
        if (!materielService.deleteById(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("deleted", true));
    }

    /**
     * POST /api/materiels/usage/salle/{salleId}?hours=2.5
     */
    @PostMapping("/usage/salle/{salleId}")
    public ResponseEntity<?> registerUsageForSalle(
            @PathVariable("salleId") Integer salleId,
            @RequestParam("hours") Double hours) {
        Optional<Map<String, List<String>>> payload = materielService.registerUsageForSalle(salleId, hours);
        if (payload.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Parameter 'hours' must be > 0."));
        }
        return ResponseEntity.ok(payload.get());
    }
}
