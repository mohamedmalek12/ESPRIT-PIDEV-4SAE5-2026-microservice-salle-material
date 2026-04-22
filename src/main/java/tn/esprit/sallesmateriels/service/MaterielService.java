package tn.esprit.sallesmateriels.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.sallesmateriels.entities.Materiel;
import tn.esprit.sallesmateriels.entities.MaterielStatus;
import tn.esprit.sallesmateriels.integration.IntegrationQueues;
import tn.esprit.sallesmateriels.repositories.MaterielRepository;
import tn.esprit.sallesmateriels.repositories.SalleRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class MaterielService {

    private static final Logger log = LoggerFactory.getLogger(MaterielService.class);
    private static final String STOMP_TOPIC_WARNINGS = "/topic/warnings";

    private final MaterielRepository materielRepository;
    private final SalleRepository salleRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RabbitTemplate rabbitTemplate;

    public MaterielService(
            MaterielRepository materielRepository,
            SalleRepository salleRepository,
            SimpMessagingTemplate messagingTemplate,
            RabbitTemplate rabbitTemplate) {
        this.materielRepository = materielRepository;
        this.salleRepository = salleRepository;
        this.messagingTemplate = messagingTemplate;
        this.rabbitTemplate = rabbitTemplate;
    }

    public List<Materiel> findAll() {
        return materielRepository.findAll();
    }

    public Optional<Materiel> findById(Integer id) {
        return materielRepository.findById(id);
    }

    public MaterielSaveOutcome create(Map<String, Object> body, Integer salleId) {
        String nom = body != null && body.get("nom") != null ? body.get("nom").toString().trim() : null;
        String statusStr = body != null && body.get("status") != null ? body.get("status").toString() : null;
        if (nom == null || nom.isEmpty()) {
            return MaterielSaveOutcome.badRequest(null);
        }
        MaterielStatus status = toStatus(statusStr);
        if (status == null) {
            status = MaterielStatus.AVAILABLE;
        }

        Materiel m = new Materiel();
        m.setNom(nom);
        m.setStatus(status);
        m.setDureeUtilisation(toDouble(body != null ? body.get("dureeUtilisation") : null, 0.0));
        m.setSeuilMaintenance(toDouble(body != null ? body.get("seuilMaintenance") : null, 100.0));
        int quantiteTotale = toInt(body != null ? body.get("quantiteTotale") : null, 1);
        int quantiteAssociee = toInt(body != null ? body.get("quantiteAssociee") : null, 0);
        if (quantiteTotale < 0 || quantiteAssociee < 0) {
            return MaterielSaveOutcome.badRequest("Quantities cannot be negative.");
        }
        if (quantiteAssociee > quantiteTotale) {
            return MaterielSaveOutcome.badRequest("Assigned quantity cannot be greater than available quantity.");
        }
        m.setQuantiteTotale(quantiteTotale);
        m.setQuantiteAssociee(quantiteAssociee);
        if (salleId != null && salleId > 0) {
            salleRepository.findById(salleId).ifPresent(m::setSalle);
        }
        Materiel saved = materielRepository.save(m);
        List<String> qtyWarnings = new ArrayList<>();
        addHighAssignmentWarnings(saved, qtyWarnings);
        publishIfNonEmpty(qtyWarnings);
        return MaterielSaveOutcome.ok(saved, qtyWarnings);
    }

    public Optional<MaterielSaveOutcome> update(Integer id, Map<String, Object> body, Integer salleId) {
        return materielRepository.findById(id)
                .map(m -> {
                    if (body != null) {
                        if (body.get("nom") != null) {
                            m.setNom(body.get("nom").toString().trim());
                        }
                        MaterielStatus s = toStatus(body.get("status") != null ? body.get("status").toString() : null);
                        if (s != null) {
                            m.setStatus(s);
                        }
                        if (body.get("dureeUtilisation") != null) {
                            m.setDureeUtilisation(toDouble(body.get("dureeUtilisation"),
                                    m.getDureeUtilisation() != null ? m.getDureeUtilisation() : 0.0));
                        }
                        if (body.get("seuilMaintenance") != null) {
                            m.setSeuilMaintenance(toDouble(body.get("seuilMaintenance"),
                                    m.getSeuilMaintenance() != null ? m.getSeuilMaintenance() : 100.0));
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
                        return MaterielSaveOutcome.badRequest("Quantities cannot be negative.");
                    }
                    if (qAssociee > qTotal) {
                        return MaterielSaveOutcome.badRequest(
                                "Assigned quantity cannot be greater than available quantity.");
                    }

                    if (salleId != null) {
                        if (salleId <= 0) {
                            m.setSalle(null);
                        } else {
                            salleRepository.findById(salleId).ifPresent(m::setSalle);
                        }
                    }
                    Materiel saved = materielRepository.save(m);
                    List<String> qtyWarnings = new ArrayList<>();
                    addHighAssignmentWarnings(saved, qtyWarnings);
                    publishIfNonEmpty(qtyWarnings);
                    return MaterielSaveOutcome.ok(saved, qtyWarnings);
                });
    }

    @Transactional
    public boolean deleteById(Integer id) {
        if (!materielRepository.existsById(id)) {
            return false;
        }
        materielRepository.deleteByIdNative(id);
        return true;
    }

    /**
     * Incrémente l'usage des matériels de la salle et remonte les alertes.
     *
     * @return empty if {@code hours} is null or &lt;= 0 (caller should respond 400), otherwise the payload for 200 OK
     */
    @Transactional
    public Optional<Map<String, List<String>>> registerUsageForSalle(Integer salleId, Double hours) {
        if (hours == null || hours <= 0) {
            return Optional.empty();
        }

        List<Materiel> materiels = materielRepository.findBySalleId(salleId);
        if (materiels.isEmpty()) {
            return Optional.of(Map.of("warnings", List.of()));
        }

        List<String> warnings = new ArrayList<>();

        for (Materiel materiel : materiels) {
            if (materiel.getStatus() == MaterielStatus.EN_MAINTENANCE) {
                String roomName = materiel.getSalle() != null && materiel.getSalle().getNom() != null
                        ? materiel.getSalle().getNom()
                        : ("id " + salleId);
                throw new RuntimeException(
                        "Material '" + materiel.getNom() + "' in room '" + roomName
                                + "' is in maintenance and cannot be used.");
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
                warnings.add("Material '" + materiel.getNom() + "' in room '" + roomName
                        + "': maintenance recommended.");
            }
        }

        materielRepository.saveAll(materiels);
        publishIfNonEmpty(warnings);
        return Optional.of(Map.of("warnings", warnings));
    }

    private void publishIfNonEmpty(List<String> warnings) {
        if (warnings == null || warnings.isEmpty()) {
            return;
        }
        MaterialWarningEvent event = buildMaterialWarningEvent(warnings);
        messagingTemplate.convertAndSend(STOMP_TOPIC_WARNINGS, event);
        forwardToClasseSeanceRabbit(warnings);
    }

    private static MaterialWarningEvent buildMaterialWarningEvent(List<String> messages) {
        return new MaterialWarningEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                "MATERIAL",
                "WARNING",
                List.copyOf(messages),
                null);
    }

    private void forwardToClasseSeanceRabbit(List<String> messages) {
        Map<String, Object> body = new HashMap<>();
        body.put("source", "MATERIAL");
        body.put("messages", messages);
        try {
            rabbitTemplate.convertAndSend("", IntegrationQueues.MATERIAL_WARNINGS, body);
        } catch (AmqpException e) {
            log.warn("Could not forward material warnings via RabbitMQ: {}", e.getMessage());
        }
    }

    private static MaterielStatus toStatus(String s) {
        if (s == null) {
            return null;
        }
        String normalized = s.trim().toUpperCase();
        if ("DISPONIBLE".equals(normalized)) {
            return MaterielStatus.AVAILABLE;
        }
        if ("EN_PANNE".equals(normalized)) {
            return MaterielStatus.OUT_OF_ORDER;
        }
        try {
            return MaterielStatus.valueOf(normalized);
        } catch (Exception e) {
            return null;
        }
    }

    private static Double toDouble(Object value, Double defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static Integer toInt(Object value, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

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

    private static final class MaterialWarningEvent {
        private final String id;
        private final Instant timestamp;
        private final String source;
        private final String severity;
        private final List<String> messages;
        private final Integer seanceId;

        private MaterialWarningEvent(
                String id,
                Instant timestamp,
                String source,
                String severity,
                List<String> messages,
                Integer seanceId) {
            this.id = id;
            this.timestamp = timestamp;
            this.source = source;
            this.severity = severity;
            this.messages = messages;
            this.seanceId = seanceId;
        }

        public String getId() {
            return id;
        }

        public Instant getTimestamp() {
            return timestamp;
        }

        public String getSource() {
            return source;
        }

        public String getSeverity() {
            return severity;
        }

        public List<String> getMessages() {
            return messages;
        }

        public Integer getSeanceId() {
            return seanceId;
        }
    }

    /**
     * Result of create / update material operations (validation vs success).
     */
    public static final class MaterielSaveOutcome {

        public enum Kind {
            OK,
            BAD_REQUEST
        }

        private final Kind kind;
        private final Materiel materiel;
        private final List<String> warnings;
        private final String errorMessage;

        private MaterielSaveOutcome(Kind kind, Materiel materiel, List<String> warnings, String errorMessage) {
            this.kind = kind;
            this.materiel = materiel;
            this.warnings = warnings != null ? warnings : Collections.emptyList();
            this.errorMessage = errorMessage;
        }

        public static MaterielSaveOutcome ok(Materiel materiel, List<String> warnings) {
            return new MaterielSaveOutcome(Kind.OK, materiel, warnings, null);
        }

        public static MaterielSaveOutcome badRequest(String message) {
            return new MaterielSaveOutcome(Kind.BAD_REQUEST, null, null, message);
        }

        public Kind getKind() {
            return kind;
        }

        public Materiel getMateriel() {
            return materiel;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
