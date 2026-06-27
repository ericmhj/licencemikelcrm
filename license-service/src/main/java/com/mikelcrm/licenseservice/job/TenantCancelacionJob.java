package com.mikelcrm.licenseservice.job;

import com.mikelcrm.licenseservice.domain.entity.Tenant;
import com.mikelcrm.licenseservice.domain.enums.EstadoTenant;
import com.mikelcrm.licenseservice.domain.repository.TenantRepository;
import com.mikelcrm.licenseservice.service.cache.CacheInvalidationService;
import com.mikelcrm.licenseservice.service.command.TenantCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Daily job at 4:00 AM that cancels tenants that have been SUSPENDED for 90+ days
 * without reactivation.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TenantCancelacionJob {

    private final TenantRepository tenantRepository;
    private final TenantCommandService tenantCommandService;
    private final CacheInvalidationService cacheInvalidationService;

    private static final int BATCH_SIZE = 500;
    private static final int DAYS_UNTIL_CANCELLATION = 90;

    @Scheduled(cron = "0 0 4 * * *")
    public void cancelInactiveTenants() {
        log.info("TenantCancelacionJob started");
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(DAYS_UNTIL_CANCELLATION);
        int totalProcessed = 0;

        List<Tenant> tenants;
        do {
            tenants = tenantRepository.findByEstadoAndFechaSuspensionBefore(
                    EstadoTenant.SUSPENDED, cutoffDate, PageRequest.of(0, BATCH_SIZE));
            for (Tenant tenant : tenants) {
                try {
                    cancelTenant(tenant);
                    totalProcessed++;
                } catch (Exception e) {
                    log.error("Error cancelling tenant {}: {}", tenant.getId(), e.getMessage(), e);
                }
            }
            // After cancellation, tenants change state so they won't reappear
        } while (tenants.size() == BATCH_SIZE);

        log.info("TenantCancelacionJob completed. Cancelled {} tenants", totalProcessed);
    }

    @Transactional
    protected void cancelTenant(Tenant tenant) {
        // Idempotency: skip if already cancelled
        if (tenant.getEstado() != EstadoTenant.SUSPENDED) {
            return;
        }

        tenantCommandService.cancelTenant(tenant.getId());
        log.info("Cancelled tenant {} after {} days of suspension. Suspended since: {}",
                tenant.getId(), DAYS_UNTIL_CANCELLATION, tenant.getFechaSuspension());
    }
}
