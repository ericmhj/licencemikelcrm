package com.mikelcrm.licenseservice.service.command;

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
import com.mikelcrm.licenseservice.exception.InvalidStateTransitionException;
import com.mikelcrm.licenseservice.exception.MontoIncorrectoException;
import com.mikelcrm.licenseservice.exception.TenantNotFoundException;
import com.mikelcrm.licenseservice.service.cache.CacheInvalidationService;
import com.mikelcrm.licenseservice.service.command.dto.CommandResponse;
import com.mikelcrm.licenseservice.service.command.dto.CreateTenantRequest;
import com.mikelcrm.licenseservice.service.command.dto.ReactivateTenantRequest;
import com.mikelcrm.licenseservice.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantCommandService {

    private final TenantRepository tenantRepository;
    private final CuotaMensualRepository cuotaMensualRepository;
    private final CuotaAlmacenamientoRepository cuotaAlmacenamientoRepository;
    private final CacheInvalidationService cacheInvalidationService;
    private final DomainEventPublisher domainEventPublisher;
    private final com.mikelcrm.licenseservice.domain.repository.PlanRepository planRepository;

    private static final Map<EstadoTenant, Set<EstadoTenant>> ALLOWED_TRANSITIONS = Map.of(
            EstadoTenant.ONBOARDING, Set.of(EstadoTenant.ACTIVE),
            EstadoTenant.ACTIVE, Set.of(EstadoTenant.SUSPENDED, EstadoTenant.CANCELLED),
            EstadoTenant.SUSPENDED, Set.of(EstadoTenant.ACTIVE, EstadoTenant.CANCELLED)
    );

    @Transactional
    public CommandResponse createTenant(CreateTenantRequest request) {
        UUID tenantId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();

        Tenant tenant = Tenant.builder()
                .id(tenantId)
                .nombre(request.getNombre())
                .emailContacto(request.getEmailContacto())
                .estado(EstadoTenant.ONBOARDING)
                .modalidadReporte(request.getModalidadApertura())
                .fechaAlta(LocalDateTime.now())
                .reportesAlmacenados(0)
                .deudaAlmacenamiento(BigDecimal.ZERO)
                .build();

        tenantRepository.save(tenant);
        log.info("Created tenant {} in ONBOARDING state. CorrelationId: {}", tenantId, correlationId);

        // El evento tenant.onboarded dispara la creación del espejo del tenant en SMT
        // (en estado onboarding) en el mismo instante en que se crea aquí.
        // Payload espejo completo (slug, plan, estado, config) para poblar public.tenants.
        Map<String, Object> payload = buildTenantMirrorPayload(tenant);
        payload.put("modalidad", request.getModalidadApertura().name());
        domainEventPublisher.publish("tenant.onboarded", tenantId, payload, correlationId.toString());

        return CommandResponse.builder()
                .id(tenantId)
                .correlationId(correlationId)
                .build();
    }

    @Transactional
    public CommandResponse activateTenant(UUID tenantId) {
        Tenant tenant = findTenant(tenantId);
        validateTransition(tenant.getEstado(), EstadoTenant.ACTIVE);

        tenant.setEstado(EstadoTenant.ACTIVE);
        tenantRepository.save(tenant);
        cacheInvalidationService.invalidateAccessCache(tenantId);

        UUID correlationId = UUID.randomUUID();
        log.info("Activated tenant {}. CorrelationId: {}", tenantId, correlationId);

        // tenant.activated marca el espejo en SMT como 'active'.
        // Se publica al topic configurado (license-events), el mismo que consume SMT.
        // Payload espejo completo para que SMT pueda provisionar/reconciliar.
        Map<String, Object> payload = buildTenantMirrorPayload(tenant);
        domainEventPublisher.publish("tenant.activated", tenantId, payload, correlationId.toString());

        return CommandResponse.builder()
                .id(tenantId)
                .correlationId(correlationId)
                .build();
    }

    @Transactional
    public CommandResponse suspendTenant(UUID tenantId) {
        Tenant tenant = findTenant(tenantId);
        validateTransition(tenant.getEstado(), EstadoTenant.SUSPENDED);

        tenant.setEstado(EstadoTenant.SUSPENDED);
        tenant.setFechaSuspension(LocalDateTime.now());
        tenantRepository.save(tenant);
        cacheInvalidationService.invalidateAccessCache(tenantId);

        UUID correlationId = UUID.randomUUID();
        log.info("Suspended tenant {}. CorrelationId: {}", tenantId, correlationId);

        Map<String, Object> payload = buildTenantMirrorPayload(tenant);
        payload.put("motivoSuspension", "cuota_impagada");
        domainEventPublisher.publish("tenant.suspended", tenantId, payload, correlationId.toString());

        return CommandResponse.builder()
                .id(tenantId)
                .correlationId(correlationId)
                .build();
    }

    @Transactional
    public CommandResponse reactivateTenant(UUID tenantId, ReactivateTenantRequest request) {
        Tenant tenant = findTenant(tenantId);
        validateTransition(tenant.getEstado(), EstadoTenant.ACTIVE);

        // Calculate total adeudo
        BigDecimal totalAdeudo = calculateTotalAdeudo(tenantId);

        // Validate monto matches exactly
        if (request.getMontoEsperado().compareTo(totalAdeudo) != 0) {
            throw new MontoIncorrectoException(totalAdeudo);
        }

        // Mark all overdue cuotas as PAGADA
        List<CuotaMensual> overdueCuotas = cuotaMensualRepository.findByTenantIdAndEstadoIn(
                tenantId, List.of(EstadoCuota.VENCIDA, EstadoCuota.EN_MORA));
        for (CuotaMensual cuota : overdueCuotas) {
            cuota.setEstado(EstadoCuota.PAGADA);
            cuota.setMontoCobrado(cuota.getMontoOriginal());
            cuota.setDescuentoPct(BigDecimal.ZERO);
            cuota.setFechaPago(LocalDateTime.now());
        }
        cuotaMensualRepository.saveAll(overdueCuotas);

        // Mark all pending storage fees as PAGADA
        List<CuotaAlmacenamiento> pendingStorage = cuotaAlmacenamientoRepository.findByTenantIdAndEstado(
                tenantId, EstadoCuotaAlmacenamiento.PENDIENTE);
        for (CuotaAlmacenamiento storage : pendingStorage) {
            storage.setEstado(EstadoCuotaAlmacenamiento.PAGADA);
        }
        cuotaAlmacenamientoRepository.saveAll(pendingStorage);

        // Transition tenant
        tenant.setEstado(EstadoTenant.ACTIVE);
        tenant.setDeudaAlmacenamiento(BigDecimal.ZERO);
        tenant.setFechaSuspension(null);
        tenantRepository.save(tenant);

        cacheInvalidationService.invalidateAccessCache(tenantId);

        UUID correlationId = UUID.randomUUID();
        log.info("Reactivated tenant {}. Monto paid: {}. CorrelationId: {}", tenantId, request.getMontoEsperado(), correlationId);

        Map<String, Object> payload = buildTenantMirrorPayload(tenant);
        payload.put("montoPagado", request.getMontoEsperado().toPlainString());
        domainEventPublisher.publish("tenant.reactivated", tenantId, payload, correlationId.toString());

        return CommandResponse.builder()
                .id(tenantId)
                .correlationId(correlationId)
                .build();
    }

    @Transactional
    public CommandResponse cancelTenant(UUID tenantId) {
        Tenant tenant = findTenant(tenantId);
        validateTransition(tenant.getEstado(), EstadoTenant.CANCELLED);

        tenant.setEstado(EstadoTenant.CANCELLED);
        tenant.setFechaCancelacion(LocalDateTime.now());
        tenantRepository.save(tenant);
        cacheInvalidationService.invalidateAccessCache(tenantId);

        UUID correlationId = UUID.randomUUID();
        log.info("Cancelled tenant {}. CorrelationId: {}", tenantId, correlationId);

        Map<String, Object> payload = buildTenantMirrorPayload(tenant);
        domainEventPublisher.publish("tenant.cancelled", tenantId, payload, correlationId.toString());

        return CommandResponse.builder()
                .id(tenantId)
                .correlationId(correlationId)
                .build();
    }

    /**
     * Actualiza atributos del tenant (nombre y/o plan) y propaga el cambio a SMT
     * vía el evento tenant.updated para mantener las columnas espejo consistentes.
     * No cambia el estado ni el slug (el slug es inmutable tras la creación).
     *
     * @param nombre nuevo nombre (null = sin cambio)
     * @param planId nuevo plan (null = sin cambio)
     */
    @Transactional
    public CommandResponse updateTenant(UUID tenantId, String nombre, UUID planId) {
        Tenant tenant = findTenant(tenantId);

        boolean changed = false;
        if (nombre != null && !nombre.isBlank() && !nombre.equals(tenant.getNombre())) {
            tenant.setNombre(nombre);
            changed = true;
        }
        if (planId != null && !planId.equals(tenant.getPlanId())) {
            tenant.setPlanId(planId);
            changed = true;
        }

        UUID correlationId = UUID.randomUUID();
        if (!changed) {
            log.info("updateTenant {}: sin cambios. CorrelationId: {}", tenantId, correlationId);
            return CommandResponse.builder().id(tenantId).correlationId(correlationId).build();
        }

        tenantRepository.save(tenant);
        cacheInvalidationService.invalidateAccessCache(tenantId);
        log.info("Updated tenant {} (nombre/plan). CorrelationId: {}", tenantId, correlationId);

        Map<String, Object> payload = buildTenantMirrorPayload(tenant);
        domainEventPublisher.publish("tenant.updated", tenantId, payload, correlationId.toString());

        return CommandResponse.builder()
                .id(tenantId)
                .correlationId(correlationId)
                .build();
    }

    private BigDecimal calculateTotalAdeudo(UUID tenantId) {
        // Sum of cuotas VENCIDA/EN_MORA
        BigDecimal cuotasVencidas = cuotaMensualRepository
                .findByTenantIdAndEstadoIn(tenantId, List.of(EstadoCuota.VENCIDA, EstadoCuota.EN_MORA))
                .stream()
                .map(CuotaMensual::getMontoOriginal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Sum of cuotas_almacenamiento PENDIENTE
        BigDecimal almacenamiento = cuotaAlmacenamientoRepository
                .findByTenantIdAndEstado(tenantId, EstadoCuotaAlmacenamiento.PENDIENTE)
                .stream()
                .map(CuotaAlmacenamiento::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return cuotasVencidas.add(almacenamiento);
    }

    private void validateTransition(EstadoTenant currentState, EstadoTenant targetState) {
        Set<EstadoTenant> allowedTargets = ALLOWED_TRANSITIONS.get(currentState);
        if (allowedTargets == null || !allowedTargets.contains(targetState)) {
            throw new InvalidStateTransitionException(currentState.name(), targetState.name());
        }
    }

    private Tenant findTenant(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
    }

    /**
     * Construye el payload espejo consistente para SMT desde la fuente de verdad.
     * Incluye identidad, slug, plan (código), estado y snapshot de metadatos (config).
     * Se usa en todos los eventos de ciclo de vida para poblar public.tenants.
     */
    private Map<String, Object> buildTenantMirrorPayload(Tenant tenant) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", tenant.getId().toString());
        payload.put("tenant_id", tenant.getId().toString());
        payload.put("nombre", tenant.getNombre());
        payload.put("slug", SlugUtil.toSlug(tenant.getNombre()));
        payload.put("admin_email", tenant.getEmailContacto());
        payload.put("estado", tenant.getEstado().name());

        // Resolver el código del plan (SMT sincroniza plan por código).
        if (tenant.getPlanId() != null) {
            planRepository.findById(tenant.getPlanId())
                    .ifPresent(plan -> payload.put("plan_codigo", plan.getCodigo()));
        }

        // Snapshot de metadatos de solo lectura para public.tenants.config.
        Map<String, Object> config = new HashMap<>();
        config.put("email_contacto", tenant.getEmailContacto());
        if (tenant.getModalidadReporte() != null) {
            config.put("modalidad_reporte", tenant.getModalidadReporte().name());
        }
        if (tenant.getFechaAlta() != null) {
            config.put("fecha_alta", tenant.getFechaAlta().toString());
        }
        payload.put("config", config);

        return payload;
    }

}
