package tn.esprit.sallesmateriels.integration;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tn.esprit.sallesmateriels.entities.Salle;
import tn.esprit.sallesmateriels.service.MaterielService;
import tn.esprit.sallesmateriels.service.SalleService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Profile("!test")
public class MaterielUsageRpcListener {

    private final MaterielService materielService;
    private final SalleService salleService;

    public MaterielUsageRpcListener(MaterielService materielService, SalleService salleService) {
        this.materielService = materielService;
        this.salleService = salleService;
    }

    @RabbitListener(queues = IntegrationQueues.MATERIEL_USAGE_RPC)
    public Map<String, Object> handleMaterielUsage(Map<String, Object> body) {
        Integer salleId = toInteger(body != null ? body.get("salleId") : null);
        Double hours = toDouble(body != null ? body.get("hours") : null);
        if (salleId == null || salleId <= 0) {
            return Map.of("invalidHours", true);
        }
        if (hours == null || hours <= 0) {
            return Map.of("invalidHours", true);
        }
        try {
            Optional<Map<String, List<String>>> result = materielService.registerUsageForSalle(salleId, hours);
            if (result.isEmpty()) {
                return Map.of("invalidHours", true);
            }
            List<String> warnings = result.get().get("warnings");
            if (warnings == null) {
                warnings = List.of();
            }
            return Map.of("warnings", warnings);
        } catch (RuntimeException e) {
            return Map.of("warnings", List.<String>of(), "error", true);
        }
    }

    @RabbitListener(queues = IntegrationQueues.SALLE_RPC)
    public Map<String, Object> handleSalleRpc(Map<String, Object> body) {
        String action = body != null && body.get("action") != null ? body.get("action").toString() : "";
        if ("all".equalsIgnoreCase(action)) {
            List<Map<String, Object>> salles = salleService.findAll().stream()
                    .map(MaterielUsageRpcListener::toSalleMap)
                    .toList();
            return Map.of("salles", salles);
        }
        if ("byId".equalsIgnoreCase(action)) {
            Integer id = toInteger(body != null ? body.get("id") : null);
            if (id == null || id <= 0) {
                Map<String, Object> response = new HashMap<>();
                response.put("salle", null);
                return response;
            }
            Map<String, Object> response = new HashMap<>();
            response.put("salle", salleService.findById(id).map(MaterielUsageRpcListener::toSalleMap).orElse(null));
            return response;
        }
        return Map.of("error", true, "message", "Unknown action.");
    }

    private static Map<String, Object> toSalleMap(Salle salle) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", salle.getId());
        map.put("nom", salle.getNom());
        map.put("capacite", salle.getCapacite());
        map.put("horsService", salle.isHorsService());
        return map;
    }

    private static Integer toInteger(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Double toDouble(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
