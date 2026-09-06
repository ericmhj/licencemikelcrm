package com.mikelcrm.licenseservice.service.command;

import com.mikelcrm.licenseservice.domain.entity.*;
import com.mikelcrm.licenseservice.domain.enums.*;
import com.mikelcrm.licenseservice.domain.repository.*;
import com.mikelcrm.licenseservice.event.DomainEventPublisher;
import com.mikelcrm.licenseservice.util.SlugUtil;
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
    private final com.mikelcrm.licenseservice.domain.repository.MensualidadTenantRepository mensualidadRepository;
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

        // 4. Aplicar en cascada: renta mensual primero, excedente a saldo a favor.
        //    Con CLABE única por tenant, el tipo de CLABE ya no determina el destino.
        aplicarCobroFijoMensual(pago, tenant, monto);

        return pago;
    }

    /**
     * Aplica un pago recibido en cascada (CLABE única por tenant):
     *   1. RENTA: cubre las mensualidades PENDIENTE completas posibles, de la más
     *      antigua a la más reciente (monto ÷ mensualidad del plan). Cada mes cubierto
     *      se registra como PAGO_RENTA y habilita el servicio.
     *   2. EXCEDENTE: el sobrante (remanente < 1 mensualidad o todo el pago si no hay
     *      mensualidades pendientes) se abona como SALDO A FAVOR (créditos, 1 MXN = 1 crédito).
     *
     * Si el tenant no tiene plan, no se puede calcular la renta: todo el pago va a
     * saldo a favor.
     */
    private void aplicarCobroFijoMensual(PagoSpeiRecibido pago, Tenant tenant, BigDecimal monto) {
        BigDecimal restante = monto;
        int mesesPagados = 0;
        java.time.LocalDate ultimoPeriodoPagado = tenant.getServicioPagadoHasta();

        Plan plan = tenant.getPlanId() != null
                ? planRepository.findById(tenant.getPlanId()).orElse(null)
                : null;
        BigDecimal mensualidad = (plan != null) ? plan.getPrecioMensual() : null;

        // ── 0. ABONO del pago completo a la cartera (prepago) ───────────────────
        // El dinero recibido entra ÍNTEGRO como saldo a favor del tenant. Luego la
        // renta se descuenta de ese saldo (ver paso 1). Así el estado de cuenta
        // refleja el abono real del cliente y no solo el excedente.
        estadoCuentaService.registrarAbono(
                tenant,
                monto,
                "Abono a cartera (pago recibido)",
                pago.getClaveRastreo(),
                pago.getClaveRastreo(),
                null,
                null,
                pago.getId()
        );

        // ── 1. RENTA: cubrir meses completos posibles ──────────────────────────
        if (mensualidad != null && mensualidad.signum() > 0) {
            // Asegurar que existan las mensualidades desde el alta hasta el mes actual.
            generarMensualidadesFaltantes(tenant, mensualidad);

            List<MensualidadTenant> pendientes = mensualidadRepository.findPendientesOrdenadas(tenant.getId());
            for (MensualidadTenant m : pendientes) {
                if (restante.compareTo(mensualidad) < 0) break; // ya no alcanza un mes completo
                m.setEstado("PAGADA");
                m.setFechaPago(LocalDateTime.now());
                m.setPagoSpeiId(pago.getId());
                m.setMonto(mensualidad);
                m.setConcepto("Pago Renta mensualidad CRM");
                mensualidadRepository.save(m);

                estadoCuentaService.registrarPagoRenta(
                        tenant,
                        mensualidad,
                        "Pago Renta mensualidad CRM - " + m.getPeriodoMes(),
                        pago.getClaveRastreo(),
                        pago.getClaveRastreo(),
                        m.getPeriodoMes().toString(),
                        pago.getId()
                );

                if (ultimoPeriodoPagado == null || m.getPeriodoMes().isAfter(ultimoPeriodoPagado)) {
                    ultimoPeriodoPagado = m.getPeriodoMes();
                }
                restante = restante.subtract(mensualidad);
                mesesPagados++;
            }
        } else {
            log.info("[SPEI] Tenant {} sin plan/mensualidad válida; todo el pago va a saldo a favor", tenant.getId());
        }

        // ── 2. Avanzar servicio pagado hasta y reactivar si quedó al corriente ──
        if (ultimoPeriodoPagado != null) {
            tenant.setServicioPagadoHasta(ultimoPeriodoPagado);
        }
        boolean reactivado = false;
        java.time.LocalDate mesActual = java.time.LocalDate.now().withDayOfMonth(1);
        boolean alCorriente = tenant.getServicioPagadoHasta() != null
                && !tenant.getServicioPagadoHasta().isBefore(mesActual);
        if (tenant.getEstado() == EstadoTenant.SUSPENDED && alCorriente) {
            tenant.setEstado(EstadoTenant.ACTIVE);
            tenant.setFechaSuspension(null);
            reactivado = true;
        }
        tenantRepository.save(tenant);
        if (reactivado) {
            cacheInvalidationService.invalidateAccessCache(tenant.getId());
            // Sincroniza el estado con SMT (espejo) vía Kafka/outbox: el pago dejó
            // al tenant al corriente, por lo que se reactiva. El payload incluye el
            // slug porque el consumidor de SMT reactiva por slug.
            Map<String, Object> payload = new HashMap<>();
            payload.put("tenantId", tenant.getId().toString());
            payload.put("tenant_id", tenant.getId().toString());
            payload.put("slug", SlugUtil.toSlug(tenant.getNombre()));
            payload.put("estado", tenant.getEstado().name());
            payload.put("motivoReactivacion", "pago_mensual");
            domainEventPublisher.publish("tenant.reactivated", tenant.getId(), payload, null);
        }

        // ── 3. EXCEDENTE: otorgar créditos por el sobrante (cartera de créditos) ─
        // El saldo a favor en el estado de cuenta YA quedó reflejado por el abono
        // total (paso 0) menos la renta descontada (paso 1); no se registra otro
        // ABONO aquí para no duplicar. Solo se otorgan los créditos operativos.
        if (restante.signum() > 0) {
            otorgarCreditos(tenant, restante, "Saldo a favor (excedente de pago)");
        }

        // ── 4. Actualizar el pago SPEI ──────────────────────────────────────────
        pago.setEstado(EstadoPagoSpei.APLICADO);
        pago.setCreditosOtorgados(restante); // solo el excedente se convirtió en créditos
        pago.setAplicadoEn(LocalDateTime.now());
        pagoSpeiRepository.save(pago);

        log.info("[SPEI] Pago aplicado: tenant={}, mesesRenta={}, excedenteCreditos={}, servicioPagadoHasta={}, reactivado={}",
                tenant.getId(), mesesPagados, restante, tenant.getServicioPagadoHasta(), reactivado);
    }

    /**
     * Genera las filas de mensualidad faltantes desde el alta del tenant (o desde
     * la última registrada) hasta el mes actual, con el monto de la mensualidad.
     */
    private void generarMensualidadesFaltantes(Tenant tenant, BigDecimal mensualidad) {
        java.time.LocalDate mesActual = java.time.LocalDate.now().withDayOfMonth(1);
        java.time.LocalDate cursor = tenant.getFechaAlta() != null
                ? tenant.getFechaAlta().toLocalDate().withDayOfMonth(1)
                : mesActual;

        while (!cursor.isAfter(mesActual)) {
            if (!mensualidadRepository.existsByTenantIdAndPeriodoMes(tenant.getId(), cursor)) {
                MensualidadTenant m = MensualidadTenant.builder()
                        .tenant(tenant)
                        .periodoMes(cursor)
                        .monto(mensualidad)
                        .estado("PENDIENTE")
                        .concepto("Pago Renta mensualidad CRM")
                        .build();
                mensualidadRepository.save(m);
            }
            cursor = cursor.plusMonths(1);
        }
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
