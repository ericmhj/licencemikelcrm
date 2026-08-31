package com.mikelcrm.licenseservice.event.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Eventos pendientes de envío (PENDING o FAILED con reintentos disponibles),
     * ordenados por creación para preservar el orden de emisión.
     */
    @Query("""
            SELECT o FROM OutboxEvent o
            WHERE o.status IN (com.mikelcrm.licenseservice.event.outbox.OutboxEvent$OutboxStatus.PENDING,
                               com.mikelcrm.licenseservice.event.outbox.OutboxEvent$OutboxStatus.FAILED)
              AND o.intentos < :maxIntentos
            ORDER BY o.creadoEn ASC
            """)
    List<OutboxEvent> findDispatchable(@Param("maxIntentos") int maxIntentos, Pageable pageable);
}
