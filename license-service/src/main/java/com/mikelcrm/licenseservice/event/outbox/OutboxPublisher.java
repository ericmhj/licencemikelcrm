package com.mikelcrm.licenseservice.event.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Relay del transactional outbox: entrega a Kafka los eventos persistidos por
 * {@link com.mikelcrm.licenseservice.event.DomainEventPublisher}.
 *
 * Se ejecuta periódicamente, envía en orden de creación, y marca cada registro
 * como SENT o FAILED. Los FAILED se reintentan hasta MAX_INTENTOS.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private static final int MAX_INTENTOS = 10;
    private static final int LOTE = 100;

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelayString = "${license-service.outbox.poll-interval-ms:2000}")
    @Transactional
    public void dispatchPending() {
        List<OutboxEvent> pendientes =
                outboxRepository.findDispatchable(MAX_INTENTOS, PageRequest.of(0, LOTE));

        if (pendientes.isEmpty()) {
            return;
        }

        for (OutboxEvent evento : pendientes) {
            try {
                // Envío síncrono: si falla, la excepción se captura y el registro
                // se marca FAILED para reintento en el siguiente ciclo.
                kafkaTemplate.send(evento.getTopic(),
                                evento.getAggregateId().toString(),
                                evento.getPayload())
                        .get();

                evento.setStatus(OutboxEvent.OutboxStatus.SENT);
                evento.setEnviadoEn(LocalDateTime.now());
                evento.setUltimoError(null);
                log.debug("Outbox {} enviado a topic {}", evento.getId(), evento.getTopic());
            } catch (Exception ex) {
                evento.setIntentos(evento.getIntentos() + 1);
                evento.setStatus(OutboxEvent.OutboxStatus.FAILED);
                evento.setUltimoError(truncate(ex.getMessage()));
                log.warn("Fallo enviando outbox {} (intento {}/{}) a topic {}: {}",
                        evento.getId(), evento.getIntentos(), MAX_INTENTOS,
                        evento.getTopic(), ex.getMessage());
            }
        }

        // Persistencia del estado actualizado dentro de la transacción del método.
        outboxRepository.saveAll(pendientes);
    }

    private static String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 2000 ? message.substring(0, 2000) : message;
    }
}
