package tn.esprit.sallesmateriels.service;

import org.springframework.stereotype.Service;
import tn.esprit.sallesmateriels.entities.Materiel;
import tn.esprit.sallesmateriels.entities.Salle;
import tn.esprit.sallesmateriels.repositories.SalleRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SalleService {

    private final SalleRepository salleRepository;

    public SalleService(SalleRepository salleRepository) {
        this.salleRepository = salleRepository;
    }

    public List<Salle> findAll() {
        return salleRepository.findAll();
    }

    public Optional<Salle> findById(Integer id) {
        return salleRepository.findById(id);
    }

    public Optional<List<Materiel>> findMaterielsBySalleId(Integer id) {
        return salleRepository.findById(id).map(Salle::getMateriels);
    }

    public Salle create(Map<String, Object> request) {
        Salle salle = new Salle();
        String nom = request != null && request.get("nom") != null ? request.get("nom").toString().trim() : "";
        Integer capacite = toInteger(request != null ? request.get("capacite") : null, 1);
        salle.setNom(nom);
        salle.setCapacite(capacite != null && capacite > 0 ? capacite : 1);
        salle.setMateriels(new ArrayList<>());
        return salleRepository.save(salle);
    }

    public Optional<Salle> update(Integer id, Map<String, Object> request) {
        return salleRepository.findById(id)
                .map(existing -> {
                    if (request != null && request.get("nom") != null) {
                        existing.setNom(request.get("nom").toString().trim());
                    }
                    Integer capacite = toInteger(request != null ? request.get("capacite") : null, null);
                    if (capacite != null && capacite > 0) {
                        existing.setCapacite(capacite);
                    }
                    return salleRepository.save(existing);
                });
    }

    private static Integer toInteger(Object value, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public SalleDeleteOutcome deleteById(Integer id) {
        if (!salleRepository.existsById(id)) {
            return SalleDeleteOutcome.NOT_FOUND;
        }
        try {
            salleRepository.deleteById(id);
            return SalleDeleteOutcome.DELETED;
        } catch (Exception e) {
            e.printStackTrace();
            return SalleDeleteOutcome.ERROR;
        }
    }

    /**
     * Result of deleting a room.
     */
    public enum SalleDeleteOutcome {
        DELETED,
        NOT_FOUND,
        ERROR
    }
}
