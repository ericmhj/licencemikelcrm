package com.mikelcrm.licenseservice.service.command;

import com.mikelcrm.licenseservice.domain.entity.ContratoAnual;
import com.mikelcrm.licenseservice.domain.entity.CuotaMensual;
import com.mikelcrm.licenseservice.domain.enums.EstadoContrato;
import com.mikelcrm.licenseservice.domain.enums.EstadoCuota;
import com.mikelcrm.licenseservice.domain.repository.ContratoAnualRepository;
import com.mikelcrm.licenseservice.domain.repository.CuotaMensualRepository;
import com.mikelcrm.licenseservice.domain.repository.TenantRepository;
import com.mikelcrm.licenseservice.event.DomainEventPublisher;
import com.mikelcrm.licenseservice.exception.ContractNotFoundException;
import com.mikelcrm.licenseservice.exception.CuotaNotFoundException;
import com.mikelcrm.licenseservice.exception.CuotaVencidaException;
import com.mikelcrm.licenseservice.exception.InvalidStateTransitionException;
import com.mikelcrm.licenseservice.exception.TenantNotFoundException;
import com.mikelcrm.licenseservice.service.cache.CacheInvalidationService;
import com.mikelcrm.licenseservice.service.command.dto.CommandResponse;
import com.mikelcrm.licenseservice.service.command.dto.PayCuotaRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CuotaPaymentService {

    private final CuotaMensualRepository cuotaMensualRepository;
    private final ContratoAnualRepository contratoRepository;
    private final TenantRepository tenantRepository;
    private final CacheInvalidationService cacheInvalidationService;
    private final DomainEventPublisher domainEventPublisher;

    private static final BigDecimal DISCOUNT_EARLY = new BigDecimal("0.90");  // 10% off
    private static final BigDecimal DISCOUNT_SAME_DAY = new BigDecimal("0.97"); // 3% off
    private static final BigDecimal DISCOUNT_PCT_10 = new BigDecimal("10.00");
    private static final BigDecimal DISCOUNT_PCT_3 = new BigDecimal("3.00");

    @Transactional
    public CommandResponse payCuota(UUID tenantId, UUID contratoId, UUID cuotaId, PayCuotaRequest request) {
        // Validate tenant exists
        tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));

        // Validate contract exists and belongs to tenant
        ContratoAnual contrato = contratoRepository.findById(contratoId)
                .orElseThrow(() -> new ContractNotFoundException(contratoId));
        if (!contrato.getTenant().getId().equals(tenantId)) {
            throw new ContractNotFoundException(contratoId);
        }

        // Validate cuota exists and belongs to contract
        CuotaMensual cuota = cuotaMensualRepository.findById(cuotaId)
                .orElseThrow(() -> new CuotaNotFoundException(cuotaId));
        if (!cuota.getContrato().getId().equals(contratoId)) {
            throw new CuotaNotFoundException(cuotaId);
        }

        // Validate cuota is PENDIENTE
        if (cuota.getEstado() != EstadoCuota.PENDIENTE) {
            throw new InvalidStateTransitionException(cuota.getEstado().name(), "PAGADA");
        }

        // Calculate discount
        LocalDate fechaPago = request.getFechaPago();
        LocalDate fechaLimite = cuota.getFechaLimite();
        BigDecimal montoOriginal = cuota.getMontoOriginal();
        BigDecimal montoCobrado;
        BigDecimal descuentoPct;

        if (fechaPago.isBefore(fechaLimite)) {
            // Before deadline: 10% discount
            montoCobrado = montoOriginal.multiply(DISCOUNT_EARLY).setScale(2, RoundingMode.HALF_UP);
            descuentoPct = DISCOUNT_PCT_10;
        } else if (fechaPago.isEqual(fechaLimite)) {
            // Same day: 3% discount
            montoCobrado = montoOriginal.multiply(DISCOUNT_SAME_DAY).setScale(2, RoundingMode.HALF_UP);
            descuentoPct = DISCOUNT_PCT_3;
        } else if (fechaPago.isEqual(fechaLimite.plusDays(1))) {
            // Grace day: no discount
            montoCobrado = montoOriginal;
            descuentoPct = BigDecimal.ZERO;
        } else {
            // Past grace day: cuota is overdue
            throw new CuotaVencidaException(
                    String.format("Cuota %s is overdue. Fecha límite was %s, payment date is %s",
                            cuotaId, fechaLimite, fechaPago));
        }

        // Update cuota
        cuota.setEstado(EstadoCuota.PAGADA);
        cuota.setMontoCobrado(montoCobrado);
        cuota.setDescuentoPct(descuentoPct);
        cuota.setFechaPago(LocalDateTime.now());
        cuotaMensualRepository.save(cuota);

        // Increment contrato.cuotas_pagadas
        contrato.setCuotasPagadas(contrato.getCuotasPagadas() + 1);

        // If first cuota (numero=1) and contrato is CREATED → transition to ACTIVE
        if (cuota.getNumero() == 1 && contrato.getEstado() == EstadoContrato.CREATED) {
            contrato.setEstado(EstadoContrato.ACTIVE);
            log.info("Contract {} transitioned to ACTIVE after first cuota payment", contratoId);
        }
        contratoRepository.save(contrato);

        cacheInvalidationService.invalidateAccessCache(tenantId);

        UUID correlationId = UUID.randomUUID();
        log.info("Paid cuota {} for contract {} of tenant {}. Monto: {} ({}% discount). CorrelationId: {}",
                cuotaId, contratoId, tenantId, montoCobrado, descuentoPct, correlationId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("cuotaId", cuotaId.toString());
        payload.put("contratoId", contratoId.toString());
        payload.put("tenantId", tenantId.toString());
        payload.put("monto", montoCobrado.toPlainString());
        payload.put("descuentoPct", descuentoPct.toPlainString());
        domainEventPublisher.publish("cuota.charged", tenantId, payload, correlationId.toString());

        return CommandResponse.builder()
                .id(cuotaId)
                .correlationId(correlationId)
                .build();
    }
}
