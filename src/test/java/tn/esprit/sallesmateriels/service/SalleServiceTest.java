package tn.esprit.sallesmateriels.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.sallesmateriels.entities.Materiel;
import tn.esprit.sallesmateriels.entities.Salle;
import tn.esprit.sallesmateriels.repositories.SalleRepository;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalleServiceTest {

    @Mock
    private SalleRepository salleRepository;

    @InjectMocks
    private SalleService salleService;

    @Test
    void testFindAll() {
        when(salleRepository.findAll()).thenReturn(List.of(new Salle()));
        List<Salle> result = salleService.findAll();
        assertEquals(1, result.size());
    }

    @Test
    void testFindById() {
        Salle salle = new Salle();
        salle.setId(1);
        when(salleRepository.findById(1)).thenReturn(Optional.of(salle));

        assertTrue(salleService.findById(1).isPresent());
        assertTrue(salleService.findById(2).isEmpty());
    }

    @Test
    void testFindMaterielsBySalleId() {
        Salle salle = new Salle();
        salle.setMateriels(new ArrayList<>());
        when(salleRepository.findById(1)).thenReturn(Optional.of(salle));

        Optional<List<Materiel>> result = salleService.findMaterielsBySalleId(1);
        assertTrue(result.isPresent());
    }

    @Test
    void testCreate_WithValidMap() {
        Map<String, Object> request = new HashMap<>();
        request.put("nom", " Salle A ");
        request.put("capacite", 30);

        when(salleRepository.save(any(Salle.class))).thenAnswer(i -> i.getArguments()[0]);

        Salle result = salleService.create(request);

        assertEquals("Salle A", result.getNom()); // Test du .trim()
        assertEquals(30, result.getCapacite());
    }

    @Test
    void testCreate_WithNullOrInvalidValues() {
        // Test avec capacite négative et nom null
        Map<String, Object> request = new HashMap<>();
        request.put("capacite", -5);

        when(salleRepository.save(any(Salle.class))).thenAnswer(i -> i.getArguments()[0]);

        Salle result = salleService.create(request);

        assertEquals("", result.getNom());
        assertEquals(1, result.getCapacite()); // Défaut à 1
    }

    @Test
    void testUpdate_ExistingSalle() {
        Salle existing = new Salle();
        existing.setId(1);
        existing.setNom("Ancien");

        Map<String, Object> request = Map.of("nom", "Nouveau", "capacite", "50");

        when(salleRepository.findById(1)).thenReturn(Optional.of(existing));
        when(salleRepository.save(any(Salle.class))).thenAnswer(i -> i.getArguments()[0]);

        Optional<Salle> updated = salleService.update(1, request);

        assertTrue(updated.isPresent());
        assertEquals("Nouveau", updated.get().getNom());
        assertEquals(50, updated.get().getCapacite());
    }

    @Test
    void testDeleteById_Scenarios() {
        // Cas 1: Not Found
        when(salleRepository.existsById(1)).thenReturn(false);
        assertEquals(SalleService.SalleDeleteOutcome.NOT_FOUND, salleService.deleteById(1));

        // Cas 2: Deleted
        when(salleRepository.existsById(2)).thenReturn(true);
        doNothing().when(salleRepository).deleteById(2);
        assertEquals(SalleService.SalleDeleteOutcome.DELETED, salleService.deleteById(2));

        // Cas 3: Error
        when(salleRepository.existsById(3)).thenReturn(true);
        doThrow(new RuntimeException()).when(salleRepository).deleteById(3);
        assertEquals(SalleService.SalleDeleteOutcome.ERROR, salleService.deleteById(3));
    }

    @Test
    void testToInteger_EdgeCases() {
        // Ce test va couvrir la méthode privée toInteger via create ou update
        Map<String, Object> request = new HashMap<>();
        request.put("capacite", "abc"); // Force le NumberFormatException

        when(salleRepository.save(any(Salle.class))).thenAnswer(i -> i.getArguments()[0]);

        Salle result = salleService.create(request);
        assertEquals(1, result.getCapacite()); // defaultValue dans create
    }
}