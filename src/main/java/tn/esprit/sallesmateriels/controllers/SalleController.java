package tn.esprit.sallesmateriels.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.sallesmateriels.dto.SalleRequest;
import tn.esprit.sallesmateriels.entities.Materiel;
import tn.esprit.sallesmateriels.entities.Salle;
import tn.esprit.sallesmateriels.repositories.SalleRepository;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;

@RestController
@RequestMapping("/api/salles")
@CrossOrigin(origins = "*")
public class SalleController {

    @Autowired
    private SalleRepository salleRepository;

    // GET toutes les salles
    @GetMapping
    public List<Salle> getAll() {
        return salleRepository.findAll();
    }

    // GET salle par ID
    @GetMapping("/{id}")
    public ResponseEntity<Salle> getById(@PathVariable("id") Integer id) {
        return salleRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // GET tous les matériels d'une salle
    // Exemple : GET /api/salles/3/materiels
    @GetMapping("/{id}/materiels")
    public ResponseEntity<List<Materiel>> getMateriels(@PathVariable("id") Integer id) {
        return salleRepository.findById(id)
                .map(salle -> ResponseEntity.ok(salle.getMateriels()))
                .orElse(ResponseEntity.notFound().build());
    }

    // POST créer une salle (DTO pour éviter 400 Bad Request)
    @PostMapping
    public Salle create(@RequestBody SalleRequest request) {
        Salle salle = new Salle();
        salle.setNom(request.getNom() != null ? request.getNom().trim() : "");
        salle.setCapacite(request.getCapacite() != null && request.getCapacite() > 0 ? request.getCapacite() : 1);
        salle.setMateriels(new java.util.ArrayList<>());
        return salleRepository.save(salle);
    }

    // PUT modifier une salle (accepte DTO ou Salle)
    @PutMapping("/{id}")
    public ResponseEntity<Salle> update(@PathVariable("id") Integer id, @RequestBody SalleRequest request) {
        return salleRepository.findById(id)
                .map(existing -> {
                    if (request.getNom() != null) existing.setNom(request.getNom().trim());
                    if (request.getCapacite() != null && request.getCapacite() > 0) existing.setCapacite(request.getCapacite());
                    return ResponseEntity.ok(salleRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE supprimer une salle (et ses matériels grâce au cascade = ALL)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) {
        if (!salleRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        try {
            salleRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
}