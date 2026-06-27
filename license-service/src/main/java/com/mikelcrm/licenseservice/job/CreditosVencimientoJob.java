package com.mikelcrm.licenseservice.job;

import com.mikelcrm.licenseservice.domain.entity.EventoCredito;
import com.mikelcrm.licenseservice.domain.entity.PaqueteCreditos;
import com.mikelcrm.licenseservice.domain.enums.EstadoPaquete;
import com.mikelcrm.licenseservice.domain.enums.TipoEventoCredito;
import com.mikelcrm.licenseservice.domain.repository.EventoCreditoRepository;
import com.mikelcrm.licenseservice.domain.repository.PaqueteCreditosRepository;
import com.mikelcrm.licenseservice.event.DomainEventPublisher;
import com.mikelcrm.licenseservice.service.cache.CacheInvalidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Daily job at 3:00 AM that expires credit packages with fecha_vencimiento = yesterday.
 * Sets saldo_disponible = 0, estado = EXPIRED, creates CADUCIDAD event, and invalidates cache.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CreditosVencimientoJob {

    private final PaqueteCreditosRepository paqueteRepository;
    private final EventoCreditoRepository eventoCreditoRepository;
    private final CacheInvalidationService cacheInvalidationService;
    private final DomainEventPublisher domainEventPublisher;

    private static final int BATCH_SIZE = 500;

    @Scheduled(cron = "0 0 3 * * *")
    public void expireCredits() {
        log.info("CreditosVencimientoJob started");
        LocalDate yesterday = LocalDate.now().minusDays(1);
        int totalProcessed = 0;

        List<PaqueteCreditos> expiredPaquetes;
        do {
            expiredPaquetes = paqueteRepository.findActiveByFechaVencimiento(yesterday, PageRequest.of(0, BATCH_SIZE));
            for (PaqueteCreditos paquete : expiredPaquetes) {
                try {
                    expirePaquete(paquete);
                    totalProcessed++;
                } catch (Exception e) {
                    log.error("Error expiring paquete {}: {}", paquete.getId(), e.getMessage(), e);
                }
            }
            // After processing, these paquetes change to EXPIRED so they won't reappear
        } while (expiredPaquetes.size() == BATCH_SIZE);

        log.info("CreditosVencimientoJob completed. Expired {} credit packages", totalProcessed);
    }

    @Transactional
    protected void expirePaquete(PaqueteCreditos paquete) {
        // Idempotency: skip if already expired
        if (paquete.getEstado() != EstadoPaquete.ACTIVE) {
            return;
        }

        BigDecimal saldoAnterior = paquete.getSaldoDisponible();

        // Set saldo to 0 and mark as EXPIRED
        paquete.setSaldoDisponible(BigDecimal.ZERO);
        paquete.setEstado(EstadoPaquete.EXPIRED);
        paqueteRepository.save(paquete);

        // Create CADUCIDAD event only if there was balance to expire
        if (saldoAnterior.compareTo(BigDecimal.ZERO) > 0) {
            EventoCredito evento = EventoCredito.builder()
                    .id(UUID.randomUUID())
                    .tenant(paquete.getTenant())
                    .paquete(paquete)
                    .tipo(TipoEventoCredito.CADUCIDAD)
                    .cantidad(saldoAnterior.negate())
                    .saldoResultante(BigDecimal.ZERO)
                    .ocurridoEn(LocalDateTime.now())
                    .build();
            eventoCreditoRepository.save(evento);
        }

        // Invalidate Redis cache
        cacheInvalidationService.invalidateAccessCache(paquete.getTenant().getId());

        // Publish credito.expired event (not in the 16 event catalog but referenced in task 9.6)
        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", paquete.getTenant().getId().toString());
        payload.put("paqueteId", paquete.getId().toString());
        payload.put("saldoExpirado", saldoAnterior.toPlainString());
        domainEventPublisher.publish("credito.expired", paquete.getTenant().getId(), payload, null);

        log.info("Expired paquete {} for tenant {}. Previous balance: {}",
                paquete.getId(), paquete.getTenant().getId(), saldoAnterior);
    }
}
