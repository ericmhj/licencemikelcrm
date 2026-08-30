package com.mikelcrm.licenseservice.job;

import com.mikelcrm.licenseservice.domain.entity.Tenant;
import com.mikelcrm.licenseservice.domain.enums.EstadoTenant;
import com.mikelcrm.licenseservice.domain.repository.TenantRepository;
import com.mikelcrm.licenseservice.event.DomainEventPublisher;
import com.mikelcrm.licenseservice.service.cache.CacheInvalidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Job diario de suspensión por impago.
 *
 * Modelo: no se generan cobros programados. El servicio se paga por SPEI y cada
 * pago marca el mes en curso como pagado (tenant.servicioPagadoHasta).
 *
 * Regla: el pago del mes se espera el día 1, con 1 día de gracia. A partir del
 * día 2 del mes, todo tenant ACTIVE que NO tenga el mes actual pagado se suspende.
 * La suspensión bloquea el ajuste manual de cartera (no es un bloqueo bancario:
 * el SPEI sigue llegando y, al pagar, el tenant se reactiva automáticamente).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CobroMensualVencidoJob {

    private final TenantRepository tenantRepository;
    private final CacheInvalidationService cacheInvalidationService;
    private final DomainEventPublisher domainEventPublisher;

    private static final int BATCH_SIZE = 500;
    private static final int DIA_CORTE = 2; // desde el día 2 se suspende a los morosos

    @Scheduled(cron = "0 0 6 * * *")
    public void suspenderMorosos() {
        LocalDate hoy = LocalDate.now();

        // Día 1 es período de espera/gracia; no se suspende a nadie.
        if (hoy.getDayOfMonth() < DIA_CORTE) {
            log.info("[SuspensionImpago] Día {} — período de gracia, no se suspende.", hoy.getDayOfMonth());
            return;
        }

        LocalDate mesActual = hoy.withDayOfMonth(1);
        log.info("[SuspensionImpago] Iniciando. Mes actual: {}", mesActual);

        int totalSuspendidos = 0;
        int page = 0;
        List<Tenant> tenants;
        do {
            tenants = tenantRepository.findByEstado(EstadoTenant.ACTIVE, PageRequest.of(page, BATCH_SIZE));
            for (Tenant tenant : tenants) {
                try {
                    // Al corriente si tiene pagado el mes actual (o uno posterior)
                    boolean alCorriente = tenant.getServicioPagadoHasta() != null
                            && !tenant.getServicioPagadoHasta().isBefore(mesActual);
                    if (!alCorriente) {
                        suspender(tenant, mesActual);
                        totalSuspendidos++;
                    }
                } catch (Exception e) {
                    log.error("[SuspensionImpago] Error procesando tenant {}: {}",
                            tenant.getId(), e.getMessage(), e);
                }
            }
            page++;
        } while (tenants.size() == BATCH_SIZE);

        log.info("[SuspensionImpago] Completado. Tenants suspendidos: {}", totalSuspendidos);
    }

    @Transactional
    protected void suspender(Tenant tenant, LocalDate mesActual) {
        tenant.setEstado(EstadoTenant.SUSPENDED);
        tenant.setFechaSuspension(LocalDateTime.now());
        tenantRepository.save(tenant);
        cacheInvalidationService.invalidateAccessCache(tenant.getId());

        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", tenant.getId().toString());
        payload.put("motivoSuspension", "impago_mensual");
        payload.put("periodoMes", mesActual.toString());
        domainEventPublisher.publish("tenant.suspended", tenant.getId(), payload, null);

        log.warn("[SuspensionImpago] Tenant {} suspendido por impago del mes {}",
                tenant.getId(), mesActual);
    }
}
