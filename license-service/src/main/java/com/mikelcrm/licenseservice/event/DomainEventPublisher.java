package com.mikelcrm.licenseservice.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mikelcrm.licenseservice.event.outbox.OutboxEvent;
import com.mikelcrm.licenseservice.event.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Publica eventos de dominio mediante el patrón transactional outbox.
 *
 * En lugar de enviar directamente a Kafka (lo que podía perder eventos si el
 * commit de la transacción de negocio ocurría pero Kafka fallaba), cada evento
 * se INSERTA en la tabla outbox_event dentro de la MISMA transacción del
 * caller ({@code @Transactional}). El {@link com.mikelcrm.licenseservice.event.outbox.OutboxPublisher}
 * lo entrega a Kafka de forma asíncrona con reintentos.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DomainEventPublisher {

    private final ObjectMapper objectMapper;
    private final OutboxEventRepository outboxRepository;

    @Value("${license-service.kafka.topic}")
    private String topic;

    @Value("${spring.profiles.active:local}")
    private String environment;

    public void publish(String eventType, UUID tenantId, Map<String, Object> payload, String correlationId) {
        publishToTopic(topic, eventType, tenantId, payload, correlationId);
    }

    /**
     * Encola un evento (envuelto en DomainEvent) hacia un topic específico.
     * Se persiste en el outbox; el envío real es asíncrono.
     */
    public void publishToTopic(String targetTopic, String eventType, UUID tenantId, Map<String, Object> payload, String correlationId) {
        DomainEvent event = DomainEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .version("1.0")
                .tenantId(tenantId)
                .occurredAt(Instant.now().toString())
                .payload(payload)
                .metadata(DomainEvent.EventMetadata.builder()
                        .correlationId(correlationId != null ? correlationId : UUID.randomUUID().toString())
                        .causationId(UUID.randomUUID().toString())
                        .service("license-service")
                        .environment(environment)
                        .build())
                .build();

        enqueue(targetTopic, eventType, tenantId, event);
    }

    /**
     * Encola un payload JSON crudo (sin envolver en DomainEvent) hacia un topic.
     * Se mantiene por compatibilidad con llamadas que enviaban el payload directo.
     */
    public void publishRawToTopic(String targetTopic, UUID tenantId, Map<String, Object> payload) {
        String eventType = payload.getOrDefault("type", "raw").toString();
        enqueue(targetTopic, eventType, tenantId, payload);
    }

    /**
     * Serializa el objeto e inserta el registro en el outbox.
     * Participa en la transacción activa del caller: si esta hace rollback,
     * el evento tampoco se persiste (consistencia atómica estado ↔ evento).
     */
    private void enqueue(String targetTopic, String eventType, UUID tenantId, Object body) {
        try {
            String json = objectMapper.writeValueAsString(body);
            OutboxEvent outbox = OutboxEvent.builder()
                    .id(UUID.randomUUID())
                    .aggregateId(tenantId)
                    .topic(targetTopic)
                    .eventType(eventType)
                    .payload(json)
                    .status(OutboxEvent.OutboxStatus.PENDING)
                    .intentos(0)
                    .build();
            outboxRepository.save(outbox);
            log.debug("Encolado evento {} para tenant {} al topic {} (outbox {})",
                    eventType, tenantId, targetTopic, outbox.getId());
        } catch (JsonProcessingException e) {
            // Falla de serialización: propagar para abortar la transacción de negocio.
            // Un evento no serializable indica un bug; no debe hacerse commit silencioso.
            log.error("Error serializando evento {} para el outbox: {}", eventType, e.getMessage());
            throw new IllegalStateException("No se pudo serializar el evento " + eventType, e);
        }
    }
}
