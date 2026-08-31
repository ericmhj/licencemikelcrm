package com.mikelcrm.licenseservice.event.outbox;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Registro del transactional outbox.
 * Se inserta en la MISMA transacción que el cambio de negocio; un publisher
 * asíncrono lo envía a Kafka y actualiza su estado.
 */
@Entity
@Table(name = "outbox_event")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    private UUID id;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(nullable = false, length = 100)
    private String topic;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private OutboxStatus status = OutboxStatus.PENDING;

    // (enum OutboxStatus definido al final de la clase)

    @Column(nullable = false)
    @Builder.Default
    private Integer intentos = 0;

    @Column(name = "creado_en", nullable = false)
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Column(name = "enviado_en")
    private LocalDateTime enviadoEn;

    @Column(name = "ultimo_error", columnDefinition = "TEXT")
    private String ultimoError;

    public enum OutboxStatus {
        PENDING, SENT, FAILED
    }
}
