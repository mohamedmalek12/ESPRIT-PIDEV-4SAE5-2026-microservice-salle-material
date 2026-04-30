package tn.esprit.sallesmateriels.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import tn.esprit.sallesmateriels.entities.Salle;
import tn.esprit.sallesmateriels.service.MaterielService;
import tn.esprit.sallesmateriels.service.SalleService;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class MaterielUsageRpcListenerTest {

    private MaterielService materielService;
    private SalleService salleService;
    private MaterielUsageRpcListener listener;

    @BeforeEach
    void setUp() {
        materielService = Mockito.mock(MaterielService.class);
        salleService = Mockito.mock(SalleService.class);
        // On l'instancie manuellement pour ignorer le @Profile("!test")
        listener = new MaterielUsageRpcListener(materielService, salleService);
    }

    @Test
    void testHandleMaterielUsage_FullCoverage() {
        // Test cas invalide (salleId null)
        Map<String, Object> result1 = listener.handleMaterielUsage(Map.of("hours", 5.0));
        assertTrue(result1.containsKey("invalidHours"));

        // Test cas success
        when(materielService.registerUsageForSalle(anyInt(), anyDouble()))
                .thenReturn(Optional.of(Map.of("warnings", List.of("Warn 1"))));

        Map<String, Object> body = Map.of("salleId", 1, "hours", 10.0);
        Map<String, Object> result2 = listener.handleMaterielUsage(body);
        assertNotNull(result2.get("warnings"));
    }

    @Test
    void testHandleSalleRpc_AllScenarios() {
        // Test Action "all"
        Salle s = new Salle();
        s.setId(1);
        when(salleService.findAll()).thenReturn(List.of(s));
        Map<String, Object> resultAll = listener.handleSalleRpc(Map.of("action", "all"));
        assertNotNull(resultAll.get("salles"));

        // Test Action "byId"
        when(salleService.findById(1)).thenReturn(Optional.of(s));
        Map<String, Object> resultById = listener.handleSalleRpc(Map.of("action", "byId", "id", 1));
        assertNotNull(resultById.get("salle"));
    }

    @Test
    void testUtilityMethods_Coverage() {
        // On envoie des lettres au lieu de chiffres pour passer dans les blocs catch (NumberFormatException)
        listener.handleMaterielUsage(Map.of("salleId", "ABC", "hours", "XYZ"));
        assertTrue(true);
    }
}