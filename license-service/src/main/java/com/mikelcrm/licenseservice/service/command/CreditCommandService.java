package com.mikelcrm.licenseservice.service.command;

import com.mikelcrm.licenseservice.domain.entity.EventoCredito;
import com.mikelcrm.licenseservice.domain.entity.PaqueteCreditos;
import com.mikelcrm.licenseservice.domain.entity.Tenant;
import com.mikelcrm.licenseservice.domain.enums.EstadoPaquete;
import com.mikelcrm.licenseservice.domain.enums.PerfilDocumento;
import com.mikelcrm.licenseservice.domain.enums.TipoEventoCredito;
import com.mikelcrm.licenseservice.domain.repository.EventoCreditoRepository;
import com.mikelcrm.licenseservice.domain.repository.PaqueteCreditosRepository;
import com.mikelcrm.licenseservice.domain.repository.TenantRepository;
import com.mikelcrm.licenseservice.event.DomainEventPublisher;
import com.mikelcrm.licenseservice.exception.PaqueteNotFoundException;
import com.mikelcrm.licenseservice.exception.SaldoInsuficienteException;
import com.mikelcrm.licenseservice.exception.TenantNotFoundException;
import com.mikelcrm.licenseservice.service.cache.CacheInvalidationService;
import com.mikelcrm.licenseservice.service.command.dto.AcquireCreditsRequest;
import com.mikelcrm.licenseservice.service.command.dto.CommandResponse;
import com.mikelcrm.licenseservice.service.command.dto.CompensateCreditsRequest;
import com.mikelcrm.licenseservice.service.command.dto.ConsumeCreditsRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditCommandService {

    private final PaqueteCreditosRepository paqueteRepository;
    private final EventoCreditoRepository eventoCreditoRepository;
    private final TenantRepository tenantRepository;
    private final CacheInvalidationService cacheInvalidationService;
    private final DomainEventPublisher domainEventPublisher;

    private static final int CREDITOS_BONUS = 4;

    private static final Map<PerfilDocumento, BigDecimal> COSTO_CREDITOS = Map.of(
            PerfilDocumento.ASISTENCIA_DIGITAL, new BigDecimal("1.0"),
            PerfilDocumento.ASISTENTE_AUTOMATICO_NORMAS, new BigDecimal("1.5")
    );

    /**
     * Task 7.1: Acquire a credit package.
     * Creates PaqueteCreditos with N credits + 4 bonus, valid for 1 year.
     */
    @Transactional
    public CommandResponse acquirePackage(UUID tenantId, AcquireCreditsRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));

        int cantidadCreditos = request.getCantidadCreditos();
        int totalAdquiridos = cantidadCreditos + CREDITOS_BONUS;
        BigDecimal saldoInicial = BigDecimal.valueOf(totalAdquiridos);

        UUID paqueteId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();

        PaqueteCreditos paquete = PaqueteCreditos.builder()
                .id(paqueteId)
                .tenant(tenant)
                .creditosPaquete(cantidadCreditos)
                .creditosBonus(CREDITOS_BONUS)
                .saldoDisponible(saldoInicial)
                .creditosTotalesAdquiridos(totalAdquiridos)
                .fechaInicio(LocalDate.now())
                .fechaVencimiento(LocalDate.now().plusYears(1))
                .estado(EstadoPaquete.ACTIVE)
                .build();

        paqueteRepository.save(paquete);

        // Create COMPRA event
        EventoCredito eventoCompra = EventoCredito.builder()
                .id(UUID.randomUUID())
                .tenant(tenant)
                .paquete(paquete)
                .tipo(TipoEventoCredito.COMPRA)
                .cantidad(BigDecimal.valueOf(cantidadCreditos))
                .saldoResultante(saldoInicial)
                .ocurridoEn(LocalDateTime.now())
                .build();
        eventoCreditoRepository.save(eventoCompra);

        // Create BONUS event
        EventoCredito eventoBonus = EventoCredito.builder()
                .id(UUID.randomUUID())
                .tenant(tenant)
                .paquete(paquete)
                .tipo(TipoEventoCredito.BONUS)
                .cantidad(BigDecimal.valueOf(CREDITOS_BONUS))
                .saldoResultante(saldoInicial)
                .ocurridoEn(LocalDateTime.now())
                .build();
        eventoCreditoRepository.save(eventoBonus);

        cacheInvalidationService.invalidateAccessCache(tenantId);

        log.info("Acquired credit package {} for tenant {}. Credits: {}, Bonus: {}, Total: {}. CorrelationId: {}",
                paqueteId, tenantId, cantidadCreditos, CREDITOS_BONUS, totalAdquiridos, correlationId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", tenantId.toString());
        payload.put("paqueteId", paqueteId.toString());
        payload.put("cantidad", CREDITOS_BONUS);
        domainEventPublisher.publish("credito.bonus_granted", tenantId, payload, correlationId.toString());

        return CommandResponse.builder()
                .id(paqueteId)
                .correlationId(correlationId)
                .build();
    }

    /**
     * Task 7.3: Consume credits with SERIALIZABLE isolation.
     * Atomic reservation using SELECT FOR UPDATE.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CommandResponse consumeCredits(UUID tenantId, ConsumeCreditsRequest request) {
        BigDecimal costo = COSTO_CREDITOS.get(request.getPerfilDocumento());

        PaqueteCreditos paquete = paqueteRepository.findActiveByTenantForUpdate(tenantId)
                .orElseThrow(() -> new PaqueteNotFoundException(tenantId));

        if (paquete.getSaldoDisponible().compareTo(costo) < 0) {
            throw new SaldoInsuficienteException(paquete.getSaldoDisponible(), costo);
        }

        BigDecimal nuevoSaldo = paquete.getSaldoDisponible().subtract(costo);
        paquete.setSaldoDisponible(nuevoSaldo);
        paqueteRepository.save(paquete);

        EventoCredito evento = EventoCredito.builder()
                .id(UUID.randomUUID())
                .tenant(paquete.getTenant())
                .paquete(paquete)
                .tipo(TipoEventoCredito.CONSUMO)
                .cantidad(costo.negate())
                .saldoResultante(nuevoSaldo)
                .perfilDocumento(request.getPerfilDocumento().name())
                .costoCreditosAplicado(costo)
                .documentoId(request.getDocumentoId())
                .usuarioId(request.getUsuarioId())
                .ocurridoEn(LocalDateTime.now())
                .build();
        eventoCreditoRepository.save(evento);

        // Task 7.12: Check balance alerts after consumption
        checkBalanceAlerts(paquete);

        cacheInvalidationService.invalidateAccessCache(tenantId);

        UUID correlationId = UUID.randomUUID();
        log.info("Consumed {} credits for tenant {}. New balance: {}. CorrelationId: {}",
                costo, tenantId, nuevoSaldo, correlationId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", tenantId.toString());
        payload.put("documentoId", request.getDocumentoId() != null ? request.getDocumentoId().toString() : null);
        payload.put("perfilDocumento", request.getPerfilDocumento().name());
        payload.put("costo", costo.toPlainString());
        payload.put("saldoResultante", nuevoSaldo.toPlainString());
        domainEventPublisher.publish("credito.consumed", tenantId, payload, correlationId.toString());

        return CommandResponse.builder()
                .id(evento.getId())
                .correlationId(correlationId)
                .build();
    }

    /**
     * Task 7.7: Compensate credits (round-trip).
     * Called when PDF generation fails after credit reservation.
     */
    @Transactional
    public CommandResponse compensateCredits(UUID tenantId, CompensateCreditsRequest request) {
        PaqueteCreditos paquete = paqueteRepository.findByTenantIdAndEstado(tenantId, EstadoPaquete.ACTIVE)
                .orElseThrow(() -> new PaqueteNotFoundException(tenantId));

        BigDecimal costo = COSTO_CREDITOS.get(request.getPerfilDocumento());
        BigDecimal nuevoSaldo = paquete.getSaldoDisponible().add(costo);
        paquete.setSaldoDisponible(nuevoSaldo);
        paqueteRepository.save(paquete);

        EventoCredito evento = EventoCredito.builder()
                .id(UUID.randomUUID())
                .tenant(paquete.getTenant())
                .paquete(paquete)
                .tipo(TipoEventoCredito.COMPENSACION)
                .cantidad(costo)
                .saldoResultante(nuevoSaldo)
                .perfilDocumento(request.getPerfilDocumento().name())
                .costoCreditosAplicado(costo)
                .documentoId(request.getDocumentoId())
                .ocurridoEn(LocalDateTime.now())
                .build();
        eventoCreditoRepository.save(evento);

        cacheInvalidationService.invalidateAccessCache(tenantId);

        UUID correlationId = UUID.randomUUID();
        log.info("Compensated {} credits for tenant {}. New balance: {}. CorrelationId: {}",
                costo, tenantId, nuevoSaldo, correlationId);

        return CommandResponse.builder()
                .id(evento.getId())
                .correlationId(correlationId)
                .build();
    }

    /**
     * Task 7.12: Check if the balance crossed a low threshold (20%, 10%, 0%).
     * Publishes credito.balance_low event when threshold is crossed.
     */
    private void checkBalanceAlerts(PaqueteCreditos paquete) {
        BigDecimal total = BigDecimal.valueOf(paquete.getCreditosTotalesAdquiridos());
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        double percentage = paquete.getSaldoDisponible().doubleValue() / total.doubleValue() * 100;
        String umbralAlcanzado = null;

        if (percentage <= 0) {
            umbralAlcanzado = "0%";
            log.warn("ALERT: Tenant {} credit balance at 0%. Balance: {}, Total acquired: {}",
                    paquete.getTenant().getId(), paquete.getSaldoDisponible(), total);
        } else if (percentage <= 10) {
            umbralAlcanzado = "10%";
            log.warn("ALERT: Tenant {} credit balance at 10% or below. Balance: {}, Total acquired: {}",
                    paquete.getTenant().getId(), paquete.getSaldoDisponible(), total);
        } else if (percentage <= 20) {
            umbralAlcanzado = "20%";
            log.info("ALERT: Tenant {} credit balance at 20% or below. Balance: {}, Total acquired: {}",
                    paquete.getTenant().getId(), paquete.getSaldoDisponible(), total);
        }

        if (umbralAlcanzado != null) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("tenantId", paquete.getTenant().getId().toString());
            payload.put("saldoActual", paquete.getSaldoDisponible().toPlainString());
            payload.put("umbralAlcanzado", umbralAlcanzado);
            domainEventPublisher.publish("credito.balance_low", paquete.getTenant().getId(), payload, null);
        }
    }
}
