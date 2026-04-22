package tn.esprit.sallesmateriels.service;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import tn.esprit.sallesmateriels.entities.Materiel;
import tn.esprit.sallesmateriels.entities.MaterielStatus;
import tn.esprit.sallesmateriels.entities.Salle;
import tn.esprit.sallesmateriels.integration.IntegrationQueues;
import tn.esprit.sallesmateriels.repositories.MaterielRepository;
import tn.esprit.sallesmateriels.repositories.SalleRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MaterielServiceTest {

    @Test
    void registerUsageForSalle_publishesThresholdWarnings() {
        MaterielRepository materielRepository = mock(MaterielRepository.class);
        SalleRepository salleRepository = mock(SalleRepository.class);
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        MaterielService service = new MaterielService(
                materielRepository, salleRepository, messagingTemplate, rabbitTemplate);

        Salle salle = new Salle();
        salle.setId(3);
        salle.setNom("Lab A");

        Materiel materiel = new Materiel();
        materiel.setNom("Projector");
        materiel.setSalle(salle);
        materiel.setStatus(MaterielStatus.AVAILABLE);
        materiel.setDureeUtilisation(95.0);
        materiel.setSeuilMaintenance(100.0);
        materiel.setQuantiteAssociee(1);

        when(materielRepository.findBySalleId(3)).thenReturn(List.of(materiel));

        Optional<Map<String, List<String>>> result = service.registerUsageForSalle(3, 10.0);

        assertTrue(result.isPresent());
        assertEquals(1, result.get().get("warnings").size());
        verify(materielRepository).saveAll(anyList());
        verify(rabbitTemplate).convertAndSend(eq(""), eq(IntegrationQueues.MATERIAL_WARNINGS), anyMap());
    }
}
