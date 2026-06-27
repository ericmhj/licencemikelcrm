package com.mikelcrm.licenseservice.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DomainEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${license-service.kafka.topic}")
    private String topic;

    @Value("${spring.profiles.active:local}")
    private String environment;

    public void publish(String eventType, UUID tenantId, Map<String, Object> payload, String correlationId) {
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

        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, tenantId.toString(), json)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish event {}: {}", eventType, ex.getMessage());
                        } else {
                            log.debug("Published event {} for tenant {}", eventType, tenantId);
                        }
                    });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event {}: {}", eventType, e.getMessage());
        }
    }
}
