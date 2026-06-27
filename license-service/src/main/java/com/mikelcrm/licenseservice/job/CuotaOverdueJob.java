package com.mikelcrm.licenseservice.job;

import com.mikelcrm.licenseservice.domain.entity.CuotaAlmacenamiento;
import com.mikelcrm.licenseservice.domain.entity.CuotaMensual;
import com.mikelcrm.licenseservice.domain.entity.Tenant;
import com.mikelcrm.licenseservice.domain.enums.EstadoCuota;
import com.mikelcrm.licenseservice.domain.enums.EstadoCuotaAlmacenamiento;
import com.mikelcrm.licenseservice.domain.enums.EstadoTenant;
import com.mikelcrm.licenseservice.domain.repository.CuotaAlmacenamientoRepository;
import com.mikelcrm.licenseservice.domain.repository.CuotaMensualRepository;
import com.mikelcrm.licenseservice.domain.repository.TenantRepository;
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
 * Daily job at 1:00 AM that detects overdue cuotas (fecha_limite + 2 days without payment).
 * Marks cuotas as EN_MORA, suspends the tenant, and generates a CuotaAlmacenamiento.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CuotaOverdueJob {

    private final CuotaMensualRepository cuotaRepository;
    private final TenantRepository tenantRepository;
    private final CuotaAlmacenamientoRepository cuotaAlmacenamientoRepository;
    private final CacheInvalidationService cacheInvalidationService;
    private final DomainEventPublisher domainEventPublisher;

    private static final int BATCH_SIZE = 500;

    @Scheduled(cron = "0 0 1 * * *")
    public void detectOverdueCuotas() {
        log.info("CuotaOverdueJob started");
        LocalDate overdueDate = LocalDate.now().minusDays(2);
        int totalProcessed = 0;

        List<CuotaMensual> batch;
        do {
            batch = cuotaRepository.findPendienteBeforeDate(overdueDate, PageRequest.of(0, BATCH_SIZE));
            for (CuotaMensual cuota : batch) {
                try {
                    processOverdueCuota(cuota);
                    totalProcessed++;
                } catch (Exception e) {
                    log.error("Error processing overdue cuota {}: {}", cuota.getId(), e.getMessage(), e);
                }
            }
        } while (batch.size() == BATCH_SIZE);

        log.info("CuotaOverdueJob completed. Processed {} overdue cuotas", totalProcessed);
    }

    @Transactional
    protected void processOverdueCuota(CuotaMensual cuota) {
        // Idempotency: skip if already EN_MORA
        if (cuota.getEstado() != EstadoCuota.PENDIENTE) {
            return;
        }

        // 1. Mark cuota as EN_MORA
        cuota.setEstado(EstadoCuota.EN_MORA);
        cuotaRepository.save(cuota);

        // 2. Publish cuota.overdue event
        Tenant tenant = cuota.getContrato().getTenant();
        Map<String, Object> overduePayload = new HashMap<>();
        overduePayload.put("cuotaId", cuota.getId().toString());
        overduePayload.put("contratoId", cuota.getContrato().getId().toString());
        overduePayload.put("tenantId", tenant.getId().toString());
        domainEventPublisher.publish("cuota.overdue", tenant.getId(), overduePayload, null);

        // 3. Suspend the tenant (ACTIVE → SUSPENDED)
        if (tenant.getEstado() == EstadoTenant.ACTIVE) {
            tenant.setEstado(EstadoTenant.SUSPENDED);
            tenant.setFechaSuspension(LocalDateTime.now());
            tenantRepository.save(tenant);
            cacheInvalidationService.invalidateAccessCache(tenant.getId());

            // Publish tenant.suspended event
            Map<String, Object> suspendedPayload = new HashMap<>();
            suspendedPayload.put("tenantId", tenant.getId().toString());
            suspendedPayload.put("motivoSuspension", "cuota_impagada");
            domainEventPublisher.publish("tenant.suspended", tenant.getId(), suspendedPayload, null);

            // 4. Generate CuotaAlmacenamiento for the tenant
            generateCuotaAlmacenamiento(tenant);

            log.info("Tenant {} suspended due to overdue cuota {}. Contract: {}",
                    tenant.getId(), cuota.getId(), cuota.getContrato().getId());
        }
    }

    private void generateCuotaAlmacenamiento(Tenant tenant) {
        LocalDate periodoMes = LocalDate.now().withDayOfMonth(1);
        BigDecimal monto = calculateStorageFee(tenant.getReportesAlmacenados());

        CuotaAlmacenamiento cuotaAlmacenamiento = CuotaAlmacenamiento.builder()
                .id(UUID.randomUUID())
                .tenant(tenant)
                .periodoMes(periodoMes)
                .reportesSnapshot(tenant.getReportesAlmacenados())
                .monto(monto)
                .estado(EstadoCuotaAlmacenamiento.PENDIENTE)
                .generadaEn(LocalDateTime.now())
                .build();

        cuotaAlmacenamientoRepository.save(cuotaAlmacenamiento);

        // Publish storage.fee_generated event
        Map<String, Object> storagePayload = new HashMap<>();
        storagePayload.put("tenantId", tenant.getId().toString());
        storagePayload.put("periodo", periodoMes.toString());
        storagePayload.put("reportes", tenant.getReportesAlmacenados());
        storagePayload.put("monto", monto.toPlainString());
        domainEventPublisher.publish("storage.fee_generated", tenant.getId(), storagePayload, null);

        log.debug("Generated CuotaAlmacenamiento for tenant {}. Reportes: {}, Monto: {}",
                tenant.getId(), tenant.getReportesAlmacenados(), monto);
    }

    /**
     * Formula: 50 + CEIL(MAX(reportes - 100, 0) / 100) * 10
     */
    BigDecimal calculateStorageFee(int reportesAlmacenados) {
        if (reportesAlmacenados <= 100) {
            return new BigDecimal("50.00");
        }
        int excess = reportesAlmacenados - 100;
        int blocks = (int) Math.ceil(excess / 100.0);
        return new BigDecimal("50.00").add(BigDecimal.valueOf(blocks * 10L));
    }
}
