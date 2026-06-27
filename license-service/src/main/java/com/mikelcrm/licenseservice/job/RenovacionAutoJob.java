package com.mikelcrm.licenseservice.job;

import com.mikelcrm.licenseservice.domain.entity.ContratoAnual;
import com.mikelcrm.licenseservice.domain.entity.Tenant;
import com.mikelcrm.licenseservice.domain.enums.EstadoTenant;
import com.mikelcrm.licenseservice.domain.repository.ContratoAnualRepository;
import com.mikelcrm.licenseservice.domain.repository.TenantRepository;
import com.mikelcrm.licenseservice.event.DomainEventPublisher;
import com.mikelcrm.licenseservice.service.cache.CacheInvalidationService;
import com.mikelcrm.licenseservice.service.command.ContractCommandService;
import com.mikelcrm.licenseservice.service.command.dto.RenewContractRequest;
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
 * Daily job at 9:00 AM that processes automatic renewals for contracts
 * expiring today with renovacion_auto = true.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RenovacionAutoJob {

    private final ContratoAnualRepository contratoRepository;
    private final ContractCommandService contractCommandService;
    private final TenantRepository tenantRepository;
    private final CacheInvalidationService cacheInvalidationService;
    private final DomainEventPublisher domainEventPublisher;

    private static final int BATCH_SIZE = 500;

    @Scheduled(cron = "0 0 9 * * *")
    public void processAutoRenewals() {
        log.info("RenovacionAutoJob started");
        LocalDate today = LocalDate.now();
        int totalProcessed = 0;
        int totalFailed = 0;

        List<ContratoAnual> contracts;
        do {
            contracts = contratoRepository.findActiveAutoRenewByFechaVencimiento(today, PageRequest.of(0, BATCH_SIZE));
            for (ContratoAnual contract : contracts) {
                try {
                    processAutoRenewal(contract);
                    totalProcessed++;
                } catch (Exception e) {
                    log.error("Auto-renewal failed for contract {}. Marking tenant for suspension: {}",
                            contract.getId(), e.getMessage(), e);
                    handleRenewalFailure(contract);
                    totalFailed++;
                }
            }
            // Break after first batch since renewed contracts change state and won't appear again
            break;
        } while (contracts.size() == BATCH_SIZE);

        log.info("RenovacionAutoJob completed. Processed: {}, Failed: {}", totalProcessed, totalFailed);
    }

    @Transactional
    protected void processAutoRenewal(ContratoAnual contract) {
        RenewContractRequest request = new RenewContractRequest("AUTO_RENEWAL_SYSTEM");
        contractCommandService.renewContract(
                contract.getTenant().getId(),
                contract.getId(),
                request
        );
        log.info("Auto-renewed contract {} for tenant {}", contract.getId(), contract.getTenant().getId());
    }

    @Transactional
    protected void handleRenewalFailure(ContratoAnual contract) {
        // If renewal fails → suspend the tenant
        Tenant tenant = contract.getTenant();
        if (tenant.getEstado() == EstadoTenant.ACTIVE) {
            tenant.setEstado(EstadoTenant.SUSPENDED);
            tenant.setFechaSuspension(LocalDateTime.now());
            tenantRepository.save(tenant);
            cacheInvalidationService.invalidateAccessCache(tenant.getId());

            // Publish tenant.suspended event
            Map<String, Object> payload = new HashMap<>();
            payload.put("tenantId", tenant.getId().toString());
            payload.put("motivoSuspension", "renovacion_fallida");
            payload.put("contratoId", contract.getId().toString());
            domainEventPublisher.publish("tenant.suspended", tenant.getId(), payload, null);

            log.warn("Tenant {} suspended due to failed auto-renewal of contract {}",
                    tenant.getId(), contract.getId());
        }
    }
}
