package com.mikelcrm.licenseservice.service.command;

import com.mikelcrm.licenseservice.domain.entity.ContratoAnual;
import com.mikelcrm.licenseservice.domain.entity.CuotaMensual;
import com.mikelcrm.licenseservice.domain.entity.Tenant;
import com.mikelcrm.licenseservice.domain.enums.EstadoContrato;
import com.mikelcrm.licenseservice.domain.enums.EstadoCuota;
import com.mikelcrm.licenseservice.domain.enums.TipoContrato;
import com.mikelcrm.licenseservice.domain.repository.ContratoAnualRepository;
import com.mikelcrm.licenseservice.domain.repository.CuotaMensualRepository;
import com.mikelcrm.licenseservice.domain.repository.TenantRepository;
import com.mikelcrm.licenseservice.event.DomainEventPublisher;
import com.mikelcrm.licenseservice.exception.*;
import com.mikelcrm.licenseservice.service.cache.CacheInvalidationService;
import com.mikelcrm.licenseservice.service.command.dto.CommandResponse;
import com.mikelcrm.licenseservice.service.command.dto.CreateContractRequest;
import com.mikelcrm.licenseservice.service.command.dto.RenewContractRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContractCommandService {

    private final ContratoAnualRepository contratoRepository;
    private final CuotaMensualRepository cuotaMensualRepository;
    private final TenantRepository tenantRepository;
    private final CacheInvalidationService cacheInvalidationService;
    private final DomainEventPublisher domainEventPublisher;

    private static final BigDecimal TARIFA_RENOVACION = new BigDecimal("99.00");
    private static final int RENEWAL_WINDOW_DAYS = 30;

    @Transactional
    public CommandResponse createContract(UUID tenantId, CreateContractRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));

        // Validate no duplicate ACTIVE contract for same module
        if (request.getTipo() == TipoContrato.MODULO && request.getModulo() != null) {
            List<ContratoAnual> activeContracts = contratoRepository.findByTenantIdAndEstado(tenantId, EstadoContrato.ACTIVE);
            boolean hasDuplicate = activeContracts.stream()
                    .anyMatch(c -> request.getModulo().equals(c.getModulo()));
            if (hasDuplicate) {
                throw new DuplicateContractException(tenantId, request.getModulo());
            }
        }

        // Validate tenant has no overdue cuotas
        List<CuotaMensual> overdueCuotas = cuotaMensualRepository.findByTenantIdAndEstadoIn(
                tenantId, List.of(EstadoCuota.VENCIDA, EstadoCuota.EN_MORA));
        if (!overdueCuotas.isEmpty()) {
            throw new OverdueCuotasException(tenantId);
        }

        UUID contratoId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        int aniversario = request.getFechaInicio().getDayOfMonth();

        ContratoAnual contrato = ContratoAnual.builder()
                .id(contratoId)
                .tenant(tenant)
                .tipo(request.getTipo())
                .modulo(request.getModulo())
                .estado(EstadoContrato.CREATED)
                .cuotaMensual(request.getCuotaMensual())
                .cuotasPagadas(0)
                .cuotasTotales(12)
                .fechaInicio(request.getFechaInicio())
                .fechaVencimiento(request.getFechaInicio().plusMonths(12))
                .fechaAniversario(aniversario)
                .renovacionAuto(request.isRenovacionAuto())
                .creadoEn(LocalDateTime.now())
                .build();

        contratoRepository.save(contrato);

        // Auto-generate 12 CuotaMensual
        List<CuotaMensual> cuotas = generateCuotas(contrato);
        cuotaMensualRepository.saveAll(cuotas);

        cacheInvalidationService.invalidateAccessCache(tenantId);

        log.info("Created contract {} for tenant {}. CorrelationId: {}", contratoId, tenantId, correlationId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("contratoId", contratoId.toString());
        payload.put("tenantId", tenantId.toString());
        payload.put("modulo", request.getModulo());
        payload.put("cuotaMensual", request.getCuotaMensual().toPlainString());
        domainEventPublisher.publish("contrato.created", tenantId, payload, correlationId.toString());

        return CommandResponse.builder()
                .id(contratoId)
                .correlationId(correlationId)
                .build();
    }

    @Transactional
    public CommandResponse requestDowngrade(UUID tenantId, UUID contratoId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));

        ContratoAnual contrato = contratoRepository.findById(contratoId)
                .orElseThrow(() -> new ContractNotFoundException(contratoId));

        // Validate contract belongs to the tenant
        if (!contrato.getTenant().getId().equals(tenantId)) {
            throw new ContractNotFoundException(contratoId);
        }

        // Validate contract is ACTIVE
        if (contrato.getEstado() != EstadoContrato.ACTIVE) {
            throw new InvalidStateTransitionException(contrato.getEstado().name(), "DOWNGRADE");
        }

        // Validate no overdue cuotas
        List<CuotaMensual> overdueCuotas = cuotaMensualRepository.findByTenantIdAndEstadoIn(
                tenantId, List.of(EstadoCuota.VENCIDA, EstadoCuota.EN_MORA));
        if (!overdueCuotas.isEmpty()) {
            throw new OverdueCuotasException(tenantId);
        }

        // Set renovacion_auto = false (effective at expiry)
        contrato.setRenovacionAuto(false);
        contratoRepository.save(contrato);

        cacheInvalidationService.invalidateAccessCache(tenantId);

        UUID correlationId = UUID.randomUUID();
        log.info("Downgrade requested for contract {} of tenant {}. CorrelationId: {}", contratoId, tenantId, correlationId);

        return CommandResponse.builder()
                .id(contratoId)
                .correlationId(correlationId)
                .build();
    }

    @Transactional
    public CommandResponse renewContract(UUID tenantId, UUID contratoId, RenewContractRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));

        ContratoAnual oldContract = contratoRepository.findById(contratoId)
                .orElseThrow(() -> new ContractNotFoundException(contratoId));

        // Validate contract belongs to the tenant
        if (!oldContract.getTenant().getId().equals(tenantId)) {
            throw new ContractNotFoundException(contratoId);
        }

        // Validate renewal window or EXPIRED state
        validateRenewalAllowed(oldContract);

        // Create new ContratoAnual
        UUID newContratoId = UUID.randomUUID();
        LocalDate newFechaInicio = oldContract.getFechaVencimiento().plusDays(1);
        LocalDate newFechaVencimiento = newFechaInicio.plusMonths(12);

        ContratoAnual newContract = ContratoAnual.builder()
                .id(newContratoId)
                .tenant(tenant)
                .tipo(oldContract.getTipo())
                .modulo(oldContract.getModulo())
                .estado(EstadoContrato.CREATED)
                .cuotaMensual(oldContract.getCuotaMensual())
                .cuotasPagadas(0)
                .cuotasTotales(12)
                .fechaInicio(newFechaInicio)
                .fechaVencimiento(newFechaVencimiento)
                .fechaAniversario(newFechaInicio.getDayOfMonth())
                .renovacionAuto(oldContract.getRenovacionAuto())
                .creadoEn(LocalDateTime.now())
                .build();

        contratoRepository.save(newContract);

        // Auto-generate 12 cuotas for the new contract
        List<CuotaMensual> newCuotas = generateCuotas(newContract);
        cuotaMensualRepository.saveAll(newCuotas);

        // Transition old contract to EXPIRED if still ACTIVE
        if (oldContract.getEstado() == EstadoContrato.ACTIVE) {
            oldContract.setEstado(EstadoContrato.EXPIRED);
            contratoRepository.save(oldContract);
        }

        cacheInvalidationService.invalidateAccessCache(tenantId);

        UUID correlationId = UUID.randomUUID();
        log.info("Renewed contract {} → {} for tenant {}. CorrelationId: {}",
                contratoId, newContratoId, tenantId, correlationId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("contratoId", contratoId.toString());
        payload.put("nuevoContratoId", newContratoId.toString());
        payload.put("tenantId", tenantId.toString());
        payload.put("tarifaRenovacion", "99.00");
        domainEventPublisher.publish("contrato.renewed", tenantId, payload, correlationId.toString());

        return CommandResponse.builder()
                .id(newContratoId)
                .correlationId(correlationId)
                .build();
    }

    private void validateRenewalAllowed(ContratoAnual contract) {
        // EXPIRED contracts can always be renewed
        if (contract.getEstado() == EstadoContrato.EXPIRED) {
            return;
        }

        // ACTIVE contracts: check renewal window (days -30 to 0)
        if (contract.getEstado() != EstadoContrato.ACTIVE) {
            throw new RenewalNotAllowedException(contract.getId(),
                    "Contract must be ACTIVE or EXPIRED to renew");
        }

        LocalDate today = LocalDate.now();
        LocalDate vencimiento = contract.getFechaVencimiento();
        long daysUntilExpiry = java.time.temporal.ChronoUnit.DAYS.between(today, vencimiento);

        if (daysUntilExpiry < 0 || daysUntilExpiry > RENEWAL_WINDOW_DAYS) {
            throw new RenewalNotAllowedException(contract.getId(),
                    String.format("Contract is not within renewal window. Days until expiry: %d", daysUntilExpiry));
        }
    }

    private List<CuotaMensual> generateCuotas(ContratoAnual contrato) {
        List<CuotaMensual> cuotas = new ArrayList<>();
        int aniversario = contrato.getFechaAniversario();

        for (int i = 0; i < 12; i++) {
            LocalDate fechaLimite = calculateFechaLimite(contrato.getFechaInicio(), i, aniversario);

            CuotaMensual cuota = CuotaMensual.builder()
                    .id(UUID.randomUUID())
                    .contrato(contrato)
                    .numero(i + 1)
                    .fechaLimite(fechaLimite)
                    .montoOriginal(contrato.getCuotaMensual())
                    .descuentoPct(BigDecimal.ZERO)
                    .estado(EstadoCuota.PENDIENTE)
                    .intentoCobro(0)
                    .build();

            cuotas.add(cuota);
        }

        return cuotas;
    }

    /**
     * Calculate the fecha_limite for cuota N, adjusted to the aniversario day.
     * Handles month-end edge cases (e.g., day 31 in a 30-day month).
     */
    private LocalDate calculateFechaLimite(LocalDate fechaInicio, int monthOffset, int aniversario) {
        LocalDate baseDate = fechaInicio.plusMonths(monthOffset);
        int maxDay = baseDate.lengthOfMonth();
        int targetDay = Math.min(aniversario, maxDay);
        return baseDate.withDayOfMonth(targetDay);
    }
}
