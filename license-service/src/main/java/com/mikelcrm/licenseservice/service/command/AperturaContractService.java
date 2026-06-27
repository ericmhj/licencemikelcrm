package com.mikelcrm.licenseservice.service.command;

import com.mikelcrm.licenseservice.domain.entity.PaqueteCreditos;
import com.mikelcrm.licenseservice.domain.entity.Tenant;
import com.mikelcrm.licenseservice.domain.enums.EstadoPaquete;
import com.mikelcrm.licenseservice.domain.enums.EstadoTenant;
import com.mikelcrm.licenseservice.domain.enums.ModalidadReporte;
import com.mikelcrm.licenseservice.domain.repository.PaqueteCreditosRepository;
import com.mikelcrm.licenseservice.domain.repository.TenantRepository;
import com.mikelcrm.licenseservice.event.DomainEventPublisher;
import com.mikelcrm.licenseservice.exception.InvalidStateTransitionException;
import com.mikelcrm.licenseservice.exception.TenantNotFoundException;
import com.mikelcrm.licenseservice.service.cache.CacheInvalidationService;
import com.mikelcrm.licenseservice.service.command.dto.AperturaRequest;
import com.mikelcrm.licenseservice.service.command.dto.CommandResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for processing contract opening (apertura) for new tenants.
 * <p>
 * Business rules:
 * - Apertura Estándar (400€): 2 welcome credits, modalidad ESTANDAR
 * - Apertura Personalizada (1,400€): 10 welcome credits, modalidad PERSONALIZADO
 * - Upgrade Estándar → Personalizada: 1,000€ difference + 8 additional credits
 * - Welcome credits do not expire before 12 months
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AperturaContractService {

    private static final BigDecimal COSTO_ESTANDAR = new BigDecimal("400.00");
    private static final BigDecimal COSTO_PERSONALIZADO = new BigDecimal("1400.00");
    private static final BigDecimal COSTO_UPGRADE = new BigDecimal("1000.00");

    private static final int CREDITOS_BIENVENIDA_ESTANDAR = 2;
    private static final int CREDITOS_BIENVENIDA_PERSONALIZADO = 10;
    private static final int CREDITOS_UPGRADE_ADICIONALES = 8;

    private final TenantRepository tenantRepository;
    private final PaqueteCreditosRepository paqueteCreditosRepository;
    private final CacheInvalidationService cacheInvalidationService;
    private final DomainEventPublisher domainEventPublisher;

    /**
     * Processes the contract opening for a tenant in ONBOARDING state.
     * Creates the welcome credit package and transitions to ACTIVE.
     *
     * @param tenantId the tenant to process apertura for
     * @param request  the apertura request with modality and payment method
     * @return command response with correlation ID
     */
    @Transactional
    public CommandResponse processApertura(UUID tenantId, AperturaRequest request) {
        Tenant tenant = findTenant(tenantId);

        // Validate tenant is in ONBOARDING state
        if (tenant.getEstado() != EstadoTenant.ONBOARDING) {
            throw new InvalidStateTransitionException(tenant.getEstado().name(), "ACTIVE");
        }

        ModalidadReporte modalidad = request.getModalidad();
        int creditosBienvenida = modalidad == ModalidadReporte.ESTANDAR
                ? CREDITOS_BIENVENIDA_ESTANDAR
                : CREDITOS_BIENVENIDA_PERSONALIZADO;

        // Create welcome credits package (expires in 12 months minimum)
        PaqueteCreditos paquete = PaqueteCreditos.builder()
                .id(UUID.randomUUID())
                .tenant(tenant)
                .creditosPaquete(creditosBienvenida)
                .creditosBonus(0) // Welcome credits don't include bonus
                .saldoDisponible(BigDecimal.valueOf(creditosBienvenida))
                .creditosTotalesAdquiridos(creditosBienvenida)
                .fechaInicio(LocalDate.now())
                .fechaVencimiento(LocalDate.now().plusYears(1))
                .estado(EstadoPaquete.ACTIVE)
                .build();
        paqueteCreditosRepository.save(paquete);

        // Set tenant modalidad and activate
        tenant.setModalidadReporte(modalidad);
        tenant.setEstado(EstadoTenant.ACTIVE);
        tenantRepository.save(tenant);

        // Invalidate Redis cache
        cacheInvalidationService.invalidateAccessCache(tenantId);

        UUID correlationId = UUID.randomUUID();
        log.info("Apertura processed for tenant {}. Modalidad={}, créditos={}. CorrelationId: {}",
                tenantId, modalidad, creditosBienvenida, correlationId);

        // Publish tenant.activated event
        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", tenantId.toString());
        payload.put("modalidad", modalidad.name());
        payload.put("creditosBienvenida", creditosBienvenida);
        payload.put("costoApertura", modalidad == ModalidadReporte.ESTANDAR
                ? COSTO_ESTANDAR.toPlainString() : COSTO_PERSONALIZADO.toPlainString());
        domainEventPublisher.publish("tenant.activated", tenantId, payload, correlationId.toString());

        return CommandResponse.builder()
                .id(tenantId)
                .correlationId(correlationId)
                .build();
    }

    /**
     * Upgrades a tenant from Estándar to Personalizado.
     * Costs 1,000€ (difference) and grants 8 additional credits.
     *
     * @param tenantId the tenant to upgrade
     * @return command response with correlation ID
     */
    @Transactional
    public CommandResponse upgradeToPersonalizado(UUID tenantId) {
        Tenant tenant = findTenant(tenantId);

        // Validate tenant is ACTIVE with ESTANDAR modality
        if (tenant.getEstado() != EstadoTenant.ACTIVE) {
            throw new InvalidStateTransitionException(tenant.getEstado().name(), "upgrade not allowed");
        }
        if (tenant.getModalidadReporte() != ModalidadReporte.ESTANDAR) {
            throw new IllegalStateException("Tenant already has PERSONALIZADO modality");
        }

        // Update modality
        tenant.setModalidadReporte(ModalidadReporte.PERSONALIZADO);
        tenantRepository.save(tenant);

        // Add 8 additional credits to existing active package or create new one
        PaqueteCreditos activePaquete = paqueteCreditosRepository
                .findByTenantIdAndEstado(tenantId, EstadoPaquete.ACTIVE)
                .orElse(null);

        if (activePaquete != null) {
            // Add 8 credits to existing package
            activePaquete.setSaldoDisponible(
                    activePaquete.getSaldoDisponible().add(BigDecimal.valueOf(CREDITOS_UPGRADE_ADICIONALES)));
            activePaquete.setCreditosTotalesAdquiridos(
                    activePaquete.getCreditosTotalesAdquiridos() + CREDITOS_UPGRADE_ADICIONALES);
            activePaquete.setCreditosPaquete(
                    activePaquete.getCreditosPaquete() + CREDITOS_UPGRADE_ADICIONALES);
            paqueteCreditosRepository.save(activePaquete);
        } else {
            // Create new package with 8 credits
            PaqueteCreditos newPaquete = PaqueteCreditos.builder()
                    .id(UUID.randomUUID())
                    .tenant(tenant)
                    .creditosPaquete(CREDITOS_UPGRADE_ADICIONALES)
                    .creditosBonus(0)
                    .saldoDisponible(BigDecimal.valueOf(CREDITOS_UPGRADE_ADICIONALES))
                    .creditosTotalesAdquiridos(CREDITOS_UPGRADE_ADICIONALES)
                    .fechaInicio(LocalDate.now())
                    .fechaVencimiento(LocalDate.now().plusYears(1))
                    .estado(EstadoPaquete.ACTIVE)
                    .build();
            paqueteCreditosRepository.save(newPaquete);
        }

        // Invalidate Redis cache
        cacheInvalidationService.invalidateAccessCache(tenantId);

        UUID correlationId = UUID.randomUUID();
        log.info("Upgrade to PERSONALIZADO for tenant {}. CorrelationId: {}", tenantId, correlationId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", tenantId.toString());
        payload.put("modalidad", ModalidadReporte.PERSONALIZADO.name());
        payload.put("creditosAdicionales", CREDITOS_UPGRADE_ADICIONALES);
        payload.put("costoUpgrade", COSTO_UPGRADE.toPlainString());
        domainEventPublisher.publish("tenant.upgraded", tenantId, payload, correlationId.toString());

        return CommandResponse.builder()
                .id(tenantId)
                .correlationId(correlationId)
                .build();
    }

    private Tenant findTenant(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
    }
}
