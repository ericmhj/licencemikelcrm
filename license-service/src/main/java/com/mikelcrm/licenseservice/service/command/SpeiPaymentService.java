package com.mikelcrm.licenseservice.service.command;

import com.mikelcrm.licenseservice.domain.entity.*;
import com.mikelcrm.licenseservice.domain.enums.*;
import com.mikelcrm.licenseservice.domain.repository.*;
import com.mikelcrm.licenseservice.event.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Procesa pagos SPEI recibidos vía webhook de Stripe.
 * Identifica al tenant por la CLABE destino y aplica:
 * - Cobro fijo mensual: marca CobroMensual como PAGADO + otorga créditos del plan
 * - Abono variable: convierte monto a créditos y los suma al paquete activo
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SpeiPaymentService {

    private final CuentaClabeTenantRepository clabeRepository;
    private final CobroMensualRepository cobroMensualRepository;
    private final PagoSpeiRecibidoRepository pagoSpeiRepository;
    private final PaqueteCreditosRepository paqueteCreditosRepository;
    private final PlanRepository planRepository;
    private final TenantRepository tenantRepository;
    private final EstadoCuentaService estadoCuentaService;
    private final com.mikelcrm.licenseservice.service.cache.CacheInvalidationService cacheInvalidationService;
    private final DomainEventPublisher domainEventPublisher;

    private static final BigDecimal MAX_SALDO_CARTERA = new BigDecimal("25000");
    private static final String KAFKA_TOPIC = "license-events";

    /**
     * Procesa un pago SPEI recibido.
     * Llamado desde el webhook controller tras validar la firma de Stripe.
     */
    @Transactional
    public PagoSpeiRecibido procesarPagoSpei(
            String clabeDestino,
            BigDecimal monto,
            String claveRastreo,
            String ordenanteNombre,
            String ordenanteClabe,
            String conceptoPago,
            String stripePaymentIntentId,
            String payloadRaw) {

        // 1. Idempotencia: verificar si ya procesamos esta clave de rastreo
        if (pagoSpeiRepository.existsByClaveRastreo(claveRastreo)) {
            log.warn("[SPEI] Pago duplicado ignorado: claveRastreo={}", claveRastreo);
            return pagoSpeiRepository.findByClaveRastreo(claveRastreo).orElse(null);
        }

        // 2. Resolver tenant por CLABE destino
        CuentaClabeTenant cuentaClabe = clabeRepository.findByClabe(clabeDestino)
                .orElse(null);

        if (cuentaClabe == null || !cuentaClabe.getActiva()) {
            log.error("[SPEI] CLABE destino no registrada o inactiva: {}", clabeDestino);
            PagoSpeiRecibido pagoRechazado = PagoSpeiRecibido.builder()
                    .clabeDestino(clabeDestino)
                    .monto(monto)
                    .claveRastreo(claveRastreo)
                    .ordenanteNombre(ordenanteNombre)
                    .ordenanteClabe(ordenanteClabe)
                    .conceptoPago(conceptoPago)
                    .tipoCobro(TipoCobro.VARIABLE_PREPAGO)
                    .estado(EstadoPagoSpei.RECHAZADO)
                    .stripePaymentIntentId(stripePaymentIntentId)
                    .payloadRaw(payloadRaw)
                    .build();
            // No podemos asignar tenant si la CLABE no existe
            return pagoSpeiRepository.save(pagoRechazado);
        }

        Tenant tenant = cuentaClabe.getTenant();
        TipoCobro tipoCobro = cuentaClabe.getTipo();

        // 3. Registrar el pago recibido
        PagoSpeiRecibido pago = PagoSpeiRecibido.builder()
                .tenant(tenant)
                .clabeDestino(clabeDestino)
                .monto(monto)
                .claveRastreo(claveRastreo)
                .ordenanteNombre(ordenanteNombre)
                .ordenanteClabe(ordenanteClabe)
                .conceptoPago(conceptoPago)
                .tipoCobro(tipoCobro)
                .estado(EstadoPagoSpei.RECIBIDO)
                .stripePaymentIntentId(stripePaymentIntentId)
                .payloadRaw(payloadRaw)
                .build();
        pago = pagoSpeiRepository.save(pago);

        // 4. Aplicar según tipo de cobro
        if (tipoCobro == TipoCobro.FIJO_MENSUAL) {
            aplicarCobroFijoMensual(pago, tenant, monto);
        } else {
            aplicarAbonoVariablePrepago(pago, tenant, monto);
        }

        return pago;
    }

    /**
     * Cobro fijo mensual: se valida contra el PLAN del tenant (no contra un cobro
     * pre-generado; ya no se programan cobros). El pago:
     *  - Marca el mes en curso como pagado (servicioPagadoHasta).
     *  - Otorga los créditos del plan y registra el ABONO en el estado de cuenta.
     *  - Reactiva al tenant si estaba SUSPENDED por impago.
     * Se acepta cualquier día/hora. Siempre se cobra el mes completo.
     */
    private void aplicarCobroFijoMensual(PagoSpeiRecibido pago, Tenant tenant, BigDecimal monto) {
        // Resolver el plan del tenant
        if (tenant.getPlanId() == null) {
            log.warn("[SPEI] Pago mensual recibido pero el tenant {} no tiene plan asignado", tenant.getId());
            pago.setEstado(EstadoPagoSpei.RECHAZADO);
            pagoSpeiRepository.save(pago);
            return;
        }

        Plan plan = planRepository.findById(tenant.getPlanId()).orElse(null);
        if (plan == null) {
            log.warn("[SPEI] Plan {} no encontrado para tenant {}", tenant.getPlanId(), tenant.getId());
            pago.setEstado(EstadoPagoSpei.RECHAZADO);
            pagoSpeiRepository.save(pago);
            return;
        }

        BigDecimal montoEsperado = plan.getPrecioMensual();

        // Validar que el monto cubra la mensualidad completa (sobrepago se ignora)
        if (monto.compareTo(montoEsperado) < 0) {
            log.warn("[SPEI] Monto insuficiente para mensualidad. Esperado: {}, Recibido: {}, Tenant: {}",
                    montoEsperado, monto, tenant.getId());
            pago.setEstado(EstadoPagoSpei.RECHAZADO);
            pagoSpeiRepository.save(pago);
            return;
        }

        java.time.LocalDate periodoMes = java.time.LocalDate.now().withDayOfMonth(1);

        // Otorgar créditos del plan
        BigDecimal creditosAOtorgar = BigDecimal.valueOf(plan.getCreditosMensuales());
        otorgarCreditos(tenant, creditosAOtorgar, "Recarga mensual - " + plan.getNombre());

        // Marcar el mes como pagado y reactivar si estaba suspendido
        tenant.setServicioPagadoHasta(periodoMes);
        boolean reactivado = false;
        if (tenant.getEstado() == EstadoTenant.SUSPENDED) {
            tenant.setEstado(EstadoTenant.ACTIVE);
            tenant.setFechaSuspension(null);
            reactivado = true;
        }
        tenantRepository.save(tenant);
        if (reactivado) {
            cacheInvalidationService.invalidateAccessCache(tenant.getId());
        }

        // Actualizar pago
        pago.setEstado(EstadoPagoSpei.APLICADO);
        pago.setCreditosOtorgados(creditosAOtorgar);
        pago.setAplicadoEn(LocalDateTime.now());
        pagoSpeiRepository.save(pago);

        // Registrar en estado de cuenta del tenant
        estadoCuentaService.registrarAbono(
                tenant,
                monto,
                "Pago mensual - " + plan.getNombre() + " - " + periodoMes,
                pago.getClaveRastreo(),
                pago.getClaveRastreo(),
                periodoMes.toString(),
                null,
                pago.getId()
        );

        log.info("[SPEI] Mensualidad aplicada: tenant={}, periodo={}, créditos={}, reactivado={}",
                tenant.getId(), periodoMes, creditosAOtorgar, reactivado);
    }

    /**
     * Abono variable prepago: convierte el monto a créditos (1 MXN = 1 crédito).
     */
    private void aplicarAbonoVariablePrepago(PagoSpeiRecibido pago, Tenant tenant, BigDecimal monto) {
        // Conversión: 1 MXN = 1 crédito (configurable en el futuro)
        BigDecimal creditos = monto;

        otorgarCreditos(tenant, creditos, "Abono prepago SPEI");

        pago.setEstado(EstadoPagoSpei.APLICADO);
        pago.setCreditosOtorgados(creditos);
        pago.setAplicadoEn(LocalDateTime.now());
        pagoSpeiRepository.save(pago);

        // Registrar en estado de cuenta del tenant
        estadoCuentaService.registrarAbono(
                tenant,
                monto,
                "Abono prepago SPEI",
                pago.getClaveRastreo(),
                pago.getClaveRastreo(),
                null,
                null,
                pago.getId()
        );

        log.info("[SPEI] Abono prepago aplicado: tenant={}, monto={}, créditos={}",
                tenant.getId(), monto, creditos);
    }

    /**
     * Suma créditos al paquete activo del tenant, respetando el tope de 25,000.
     */
    private void otorgarCreditos(Tenant tenant, BigDecimal creditos, String concepto) {
        Optional<PaqueteCreditos> paqueteOpt = paqueteCreditosRepository
                .findByTenantIdAndEstado(tenant.getId(), EstadoPaquete.ACTIVE);

        if (paqueteOpt.isEmpty()) {
            log.error("[SPEI] Tenant {} no tiene paquete de créditos activo. No se pueden otorgar créditos.", tenant.getId());
            return;
        }

        PaqueteCreditos paquete = paqueteOpt.get();
        BigDecimal saldoActual = paquete.getSaldoDisponible();
        BigDecimal nuevoSaldo = saldoActual.add(creditos);

        // Tope máximo de cartera
        if (nuevoSaldo.compareTo(MAX_SALDO_CARTERA) > 0) {
            nuevoSaldo = MAX_SALDO_CARTERA;
            creditos = MAX_SALDO_CARTERA.subtract(saldoActual);
            log.warn("[SPEI] Tope de cartera alcanzado para tenant {}. Créditos aplicados limitados a: {}",
                    tenant.getId(), creditos);
        }

        paquete.setSaldoDisponible(nuevoSaldo);
        paqueteCreditosRepository.save(paquete);

        // Publicar evento Kafka para sincronizar con SMT
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "credit.ledger.entry");
        payload.put("tenant_id", tenant.getId().toString());
        payload.put("nombre", tenant.getNombre());

        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("id", UUID.randomUUID().toString());
        entry.put("tipo", "recarga");
        entry.put("cantidad", creditos.doubleValue());
        entry.put("saldo_resultante", nuevoSaldo.doubleValue());
        entry.put("concepto", concepto);
        payload.put("entry", entry);
        payload.put("timestamp", java.time.Instant.now().toString());

        domainEventPublisher.publishRawToTopic(KAFKA_TOPIC, tenant.getId(), payload);
    }
}
