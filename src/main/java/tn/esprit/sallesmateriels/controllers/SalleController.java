package tn.esprit.sallesmateriels.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.sallesmateriels.entities.Materiel;
import tn.esprit.sallesmateriels.entities.Salle;
import tn.esprit.sallesmateriels.service.SalleService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/salles")
public class SalleController {

    private final SalleService salleService;

    public SalleController(SalleService salleService) {
        this.salleService = salleService;
    }

    @GetMapping
    public List<Salle> getAll() {
        return salleService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Salle> getById(@PathVariable("id") Integer id) {
        return salleService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/materiels")
    public ResponseEntity<List<Materiel>> getMateriels(@PathVariable("id") Integer id) {
        return salleService.findMaterielsBySalleId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Salle create(@RequestBody Map<String, Object> request) {
        return salleService.create(request);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Salle> update(@PathVariable("id") Integer id, @RequestBody Map<String, Object> request) {
        return salleService.update(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) {
        return switch (salleService.deleteById(id)) {
            case NOT_FOUND -> ResponseEntity.notFound().build();
            case ERROR -> ResponseEntity.status(500).build();
            case DELETED -> ResponseEntity.noContent().build();
        };
    }
}
