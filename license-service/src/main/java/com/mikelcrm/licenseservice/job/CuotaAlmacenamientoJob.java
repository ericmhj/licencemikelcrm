package com.mikelcrm.licenseservice.job;

import com.mikelcrm.licenseservice.domain.entity.CuotaAlmacenamiento;
import com.mikelcrm.licenseservice.domain.entity.Tenant;
import com.mikelcrm.licenseservice.domain.enums.EstadoCuotaAlmacenamiento;
import com.mikelcrm.licenseservice.domain.enums.EstadoTenant;
import com.mikelcrm.licenseservice.domain.repository.CuotaAlmacenamientoRepository;
import com.mikelcrm.licenseservice.domain.repository.TenantRepository;
import com.mikelcrm.licenseservice.event.DomainEventPublisher;
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
 * Monthly job on the 1st at 2:00 AM that generates CuotaAlmacenamiento
 * for all tenants in SUSPENDED state.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CuotaAlmacenamientoJob {

    private final TenantRepository tenantRepository;
    private final CuotaAlmacenamientoRepository cuotaAlmacenamientoRepository;
    private final DomainEventPublisher domainEventPublisher;

    private static final int BATCH_SIZE = 500;

    @Scheduled(cron = "0 0 2 1 * *")
    public void generateStorageFees() {
        log.info("CuotaAlmacenamientoJob started");
        LocalDate periodoMes = LocalDate.now().withDayOfMonth(1);
        int totalProcessed = 0;

        List<Tenant> suspendedTenants;
        int page = 0;
        do {
            suspendedTenants = tenantRepository.findByEstado(EstadoTenant.SUSPENDED, PageRequest.of(page, BATCH_SIZE));
            for (Tenant tenant : suspendedTenants) {
                try {
                    processStorageFee(tenant, periodoMes);
                    totalProcessed++;
                } catch (Exception e) {
                    log.error("Error generating storage fee for tenant {}: {}", tenant.getId(), e.getMessage(), e);
                }
            }
            page++;
        } while (suspendedTenants.size() == BATCH_SIZE);

        log.info("CuotaAlmacenamientoJob completed. Generated {} storage fees", totalProcessed);
    }

    @Transactional
    protected void processStorageFee(Tenant tenant, LocalDate periodoMes) {
        // Idempotency: check if a cuota already exists for this tenant and period
        List<CuotaAlmacenamiento> existing = cuotaAlmacenamientoRepository.findByTenantId(tenant.getId());
        boolean alreadyGenerated = existing.stream()
                .anyMatch(c -> c.getPeriodoMes().equals(periodoMes));
        if (alreadyGenerated) {
            log.debug("Storage fee already generated for tenant {} period {}", tenant.getId(), periodoMes);
            return;
        }

        BigDecimal monto = calculateStorageFee(tenant.getReportesAlmacenados());

        CuotaAlmacenamiento cuota = CuotaAlmacenamiento.builder()
                .id(UUID.randomUUID())
                .tenant(tenant)
                .periodoMes(periodoMes)
                .reportesSnapshot(tenant.getReportesAlmacenados())
                .monto(monto)
                .estado(EstadoCuotaAlmacenamiento.PENDIENTE)
                .generadaEn(LocalDateTime.now())
                .build();

        cuotaAlmacenamientoRepository.save(cuota);

        // Publish storage.fee_generated event
        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", tenant.getId().toString());
        payload.put("periodo", periodoMes.toString());
        payload.put("reportes", tenant.getReportesAlmacenados());
        payload.put("monto", monto.toPlainString());
        domainEventPublisher.publish("storage.fee_generated", tenant.getId(), payload, null);

        log.debug("Generated storage fee for tenant {}. Reportes: {}, Monto: {}",
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
