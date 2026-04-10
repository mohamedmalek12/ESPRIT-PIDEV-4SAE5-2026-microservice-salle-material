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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    public ResponseEntity<?> create(
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
        m.setDureeUtilisation(toDouble(body != null ? body.get("dureeUtilisation") : null, 0.0));
        m.setSeuilMaintenance(toDouble(body != null ? body.get("seuilMaintenance") : null, 100.0));
        int quantiteTotale = toInt(body != null ? body.get("quantiteTotale") : null, 1);
        int quantiteAssociee = toInt(body != null ? body.get("quantiteAssociee") : null, 0);
        if (quantiteTotale < 0 || quantiteAssociee < 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Quantities cannot be negative."));
        }
        if (quantiteAssociee > quantiteTotale) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Assigned quantity cannot be greater than available quantity."));
        }
        m.setQuantiteTotale(quantiteTotale);
        m.setQuantiteAssociee(quantiteAssociee);
        if (salleId != null && salleId > 0) {
            salleRepository.findById(salleId).ifPresent(m::setSalle);
        }
        Materiel saved = materielRepository.save(m);
        List<String> qtyWarnings = new ArrayList<>();
        addHighAssignmentWarnings(saved, qtyWarnings);
        return ResponseEntity.ok(Map.of("materiel", saved, "warnings", qtyWarnings));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable("id") Integer id,
            @RequestBody java.util.Map<String, Object> body,
            @RequestParam(value = "salleId", required = false) Integer salleId) {
        return materielRepository.findById(id)
                .map(m -> {
                    if (body != null) {
                        if (body.get("nom") != null) m.setNom(body.get("nom").toString().trim());
                        MaterielStatus s = toStatus(body.get("status") != null ? body.get("status").toString() : null);
                        if (s != null) m.setStatus(s);
                        if (body.get("dureeUtilisation") != null) {
                            m.setDureeUtilisation(toDouble(body.get("dureeUtilisation"), m.getDureeUtilisation() != null ? m.getDureeUtilisation() : 0.0));
                        }
                        if (body.get("seuilMaintenance") != null) {
                            m.setSeuilMaintenance(toDouble(body.get("seuilMaintenance"), m.getSeuilMaintenance() != null ? m.getSeuilMaintenance() : 100.0));
                        }
                        if (body.get("quantiteTotale") != null) {
                            m.setQuantiteTotale(toInt(body.get("quantiteTotale"),
                                    m.getQuantiteTotale() != null ? m.getQuantiteTotale() : 1));
                        }
                        if (body.get("quantiteAssociee") != null) {
                            m.setQuantiteAssociee(toInt(body.get("quantiteAssociee"),
                                    m.getQuantiteAssociee() != null ? m.getQuantiteAssociee() : 0));
                        }
                    }

                    int qTotal = m.getQuantiteTotale() != null ? m.getQuantiteTotale() : 0;
                    int qAssociee = m.getQuantiteAssociee() != null ? m.getQuantiteAssociee() : 0;
                    if (qTotal < 0 || qAssociee < 0) {
                        return ResponseEntity.badRequest().body(Map.of("message", "Quantities cannot be negative."));
                    }
                    if (qAssociee > qTotal) {
                        return ResponseEntity.badRequest()
                                .body(Map.of("message", "Assigned quantity cannot be greater than available quantity."));
                    }

                    if (salleId != null) {
                        if (salleId <= 0) m.setSalle(null);
                        else salleRepository.findById(salleId).ifPresent(m::setSalle);
                    }
                    Materiel saved = materielRepository.save(m);
                    List<String> qtyWarnings = new ArrayList<>();
                    addHighAssignmentWarnings(saved, qtyWarnings);
                    return ResponseEntity.ok(Map.of("materiel", saved, "warnings", qtyWarnings));
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

    /**
     * POST /api/materiels/usage/salle/{salleId}?hours=2.5
     * Incrémente l'usage des matériels de la salle et remonte les alertes.
     */
    @PostMapping("/usage/salle/{salleId}")
    @Transactional
    public ResponseEntity<?> registerUsageForSalle(
            @PathVariable("salleId") Integer salleId,
            @RequestParam("hours") Double hours) {
        if (hours == null || hours <= 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Parameter 'hours' must be > 0."));
        }

        List<Materiel> materiels = materielRepository.findBySalleId(salleId);
        if (materiels.isEmpty()) {
            return ResponseEntity.ok(Map.of("warnings", List.of()));
        }

        List<String> warnings = new ArrayList<>();

        for (Materiel materiel : materiels) {
            if (materiel.getStatus() == MaterielStatus.EN_MAINTENANCE) {
                String roomName = materiel.getSalle() != null && materiel.getSalle().getNom() != null
                        ? materiel.getSalle().getNom()
                        : ("id " + salleId);
                throw new RuntimeException(
                        "Material '" + materiel.getNom() + "' in room '" + roomName + "' is in maintenance and cannot be used.");
            }

            double currentUsage = materiel.getDureeUtilisation() != null ? materiel.getDureeUtilisation() : 0.0;
            double threshold = materiel.getSeuilMaintenance() != null && materiel.getSeuilMaintenance() > 0
                    ? materiel.getSeuilMaintenance()
                    : 100.0;
            int quantityFactor = materiel.getQuantiteAssociee() != null && materiel.getQuantiteAssociee() > 0
                    ? materiel.getQuantiteAssociee()
                    : 1;

            double newUsage = currentUsage + (hours * quantityFactor);
            materiel.setDureeUtilisation(newUsage);

            if (newUsage >= threshold * 1.2) {
                materiel.setStatus(MaterielStatus.EN_MAINTENANCE);
                String roomName = materiel.getSalle() != null && materiel.getSalle().getNom() != null
                        ? materiel.getSalle().getNom()
                        : ("id " + salleId);
                warnings.add("Material '" + materiel.getNom() + "' in room '" + roomName
                        + "': maintenance recommended (usage > 120% of threshold).");
            } else if (newUsage >= threshold) {
                String roomName = materiel.getSalle() != null && materiel.getSalle().getNom() != null
                        ? materiel.getSalle().getNom()
                        : ("id " + salleId);
                warnings.add("Material '" + materiel.getNom() + "' in room '" + roomName + "': maintenance recommended.");
            }
        }

        materielRepository.saveAll(materiels);
        return ResponseEntity.ok(Map.of("warnings", warnings));
    }

    private static MaterielStatus toStatus(String s) {
        if (s == null) return null;
        String normalized = s.trim().toUpperCase();
        if ("DISPONIBLE".equals(normalized)) return MaterielStatus.AVAILABLE;
        if ("EN_PANNE".equals(normalized)) return MaterielStatus.OUT_OF_ORDER;
        try {
            return MaterielStatus.valueOf(normalized);
        } catch (Exception e) {
            return null;
        }
    }

    private static Double toDouble(Object value, Double defaultValue) {
        if (value == null) return defaultValue;
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static Integer toInt(Object value, Integer defaultValue) {
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * When at least 90% of total quantity is assigned, warn that little stock remains (e.g. ~10%).
     */
    private static void addHighAssignmentWarnings(Materiel materiel, List<String> warnings) {
        int total = materiel.getQuantiteTotale() != null ? materiel.getQuantiteTotale() : 0;
        int assoc = materiel.getQuantiteAssociee() != null ? materiel.getQuantiteAssociee() : 0;
        if (total <= 0 || assoc > total) {
            return;
        }
        double ratio = assoc / (double) total;
        if (ratio < 0.9) {
            return;
        }
        int remain = total - assoc;
        double pctRemain = (remain * 100.0) / total;
        String name = materiel.getNom() != null ? materiel.getNom() : "Equipment";
        warnings.add(String.format(
                "Material '%s': %.0f%% of quantity is assigned; only ~%.0f%% remains (%d of %d units still available).",
                name, ratio * 100, pctRemain, remain, total));
    }
}
