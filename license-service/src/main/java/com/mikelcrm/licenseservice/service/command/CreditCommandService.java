package com.mikelcrm.licenseservice.service.command;

import com.mikelcrm.licenseservice.domain.entity.EventoCredito;
import com.mikelcrm.licenseservice.domain.entity.PaqueteCreditos;
import com.mikelcrm.licenseservice.domain.entity.Tenant;
import com.mikelcrm.licenseservice.domain.enums.EstadoPaquete;
import com.mikelcrm.licenseservice.domain.enums.PerfilDocumento;
import com.mikelcrm.licenseservice.domain.enums.TipoEventoCredito;
import com.mikelcrm.licenseservice.domain.entity.Plan;
import com.mikelcrm.licenseservice.domain.repository.EventoCreditoRepository;
import com.mikelcrm.licenseservice.domain.repository.PaqueteCreditosRepository;
import com.mikelcrm.licenseservice.domain.repository.PlanRepository;
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
import com.mikelcrm.licenseservice.service.command.dto.ConsumeReportRequest;
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
    private final PlanRepository planRepository;
    private final CacheInvalidationService cacheInvalidationService;
    private final DomainEventPublisher domainEventPublisher;
    private final EstadoCuentaService estadoCuentaService;

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
     * Consume créditos por la generación de un reporte de estudio.
     *
     * Costo total = costoReporte(plan) + costoPuntoMuestreo(plan) * numeroPuntos.
     * Los costos se toman del PLAN contratado por el tenant (varían por plan).
     * Se descuenta de la cartera (paquete de créditos activo). NO afecta el
     * estado de cuenta (que solo registra dinero real entrante, no consumo).
     *
     * Usa isolation SERIALIZABLE + SELECT FOR UPDATE para evitar condiciones
     * de carrera sobre el saldo.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CommandResponse consumeReport(UUID tenantId, ConsumeReportRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));

        // 1. Resolver el plan del tenant para obtener las tarifas
        if (tenant.getPlanId() == null) {
            throw new IllegalStateException("El tenant " + tenantId + " no tiene un plan asignado");
        }
        Plan plan = planRepository.findById(tenant.getPlanId())
                .orElseThrow(() -> new IllegalStateException(
                        "El plan del tenant " + tenantId + " no existe"));

        // 2. Calcular costo total = costoReporte + costoPuntoMuestreo * numeroPuntos
        int numeroPuntos = request.getNumeroPuntos() != null ? request.getNumeroPuntos() : 0;
        BigDecimal costoReporte = BigDecimal.valueOf(plan.getCostoReporte());
        BigDecimal costoPuntos = BigDecimal.valueOf(plan.getCostoPuntoMuestreo())
                .multiply(BigDecimal.valueOf(numeroPuntos));
        BigDecimal costoTotal = costoReporte.add(costoPuntos);

        // 3. Validar saldo y descontar de la cartera
        PaqueteCreditos paquete = paqueteRepository.findActiveByTenantForUpdate(tenantId)
                .orElseThrow(() -> new PaqueteNotFoundException(tenantId));

        if (paquete.getSaldoDisponible().compareTo(costoTotal) < 0) {
            throw new SaldoInsuficienteException(paquete.getSaldoDisponible(), costoTotal);
        }

        BigDecimal nuevoSaldo = paquete.getSaldoDisponible().subtract(costoTotal);
        paquete.setSaldoDisponible(nuevoSaldo);
        paqueteRepository.save(paquete);

        // 4. Registrar el evento de consumo con desglose
        EventoCredito evento = EventoCredito.builder()
                .id(UUID.randomUUID())
                .tenant(tenant)
                .paquete(paquete)
                .tipo(TipoEventoCredito.CONSUMO)
                .cantidad(costoTotal.negate())
                .saldoResultante(nuevoSaldo)
                .perfilDocumento("REPORTE")
                .costoCreditosAplicado(costoTotal)
                .documentoId(request.getDocumentoId())
                .usuarioId(request.getUsuarioId())
                .ocurridoEn(LocalDateTime.now())
                .build();
        eventoCreditoRepository.save(evento);

        // 5. Registrar el CARGO en el estado de cuenta para mantener consistencia
        //    entre cartera y estado de cuenta. El concepto incluye el desglose.
        String concepto = String.format(
                "Consumo de reporte (%d punto(s) de muestreo): %s base + %s x %d puntos",
                numeroPuntos, costoReporte.toPlainString(),
                BigDecimal.valueOf(plan.getCostoPuntoMuestreo()).toPlainString(), numeroPuntos);
        estadoCuentaService.registrarCargo(
                tenant,
                costoTotal,   // monto positivo; registrarCargo lo negativiza
                concepto,
                null,         // periodoMes (no aplica)
                null          // cobroMensualId (no aplica)
        );

        checkBalanceAlerts(paquete);
        cacheInvalidationService.invalidateAccessCache(tenantId);

        UUID correlationId = UUID.randomUUID();
        log.info("[Reporte] Consumo aplicado: tenant={}, puntos={}, costoReporte={}, costoPuntos={}, total={}, nuevoSaldo={}",
                tenantId, numeroPuntos, costoReporte, costoPuntos, costoTotal, nuevoSaldo);

        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", tenantId.toString());
        payload.put("documentoId", request.getDocumentoId() != null ? request.getDocumentoId().toString() : null);
        payload.put("numeroPuntos", numeroPuntos);
        payload.put("costoReporte", costoReporte.toPlainString());
        payload.put("costoPuntos", costoPuntos.toPlainString());
        payload.put("costoTotal", costoTotal.toPlainString());
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
