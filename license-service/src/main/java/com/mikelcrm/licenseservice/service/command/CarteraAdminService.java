package com.mikelcrm.licenseservice.service.command;

import com.mikelcrm.licenseservice.controller.dto.*;
import com.mikelcrm.licenseservice.domain.entity.EstadoCuentaTenant;
import com.mikelcrm.licenseservice.domain.entity.EventoCredito;
import com.mikelcrm.licenseservice.domain.entity.PaqueteCreditos;
import com.mikelcrm.licenseservice.domain.entity.Plan;
import com.mikelcrm.licenseservice.domain.entity.Tenant;
import com.mikelcrm.licenseservice.domain.enums.EstadoPaquete;
import com.mikelcrm.licenseservice.domain.enums.TipoEventoCredito;
import com.mikelcrm.licenseservice.domain.repository.EventoCreditoRepository;
import com.mikelcrm.licenseservice.domain.repository.PaqueteCreditosRepository;
import com.mikelcrm.licenseservice.domain.repository.PlanRepository;
import com.mikelcrm.licenseservice.domain.repository.TenantRepository;
import com.mikelcrm.licenseservice.event.DomainEventPublisher;
import com.mikelcrm.licenseservice.exception.TenantNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for administrating tenant credit wallets.
 * Only called by platform_admin users through CarteraAdminController.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CarteraAdminService {

    private final TenantRepository tenantRepository;
    private final PaqueteCreditosRepository paqueteCreditosRepository;
    private final EventoCreditoRepository eventoCreditoRepository;
    private final PlanRepository planRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final EstadoCuentaService estadoCuentaService;

    private static final String KAFKA_TOPIC = "license-events";

    /**
     * List all tenants (summary view for platform admin).
     */
    public List<TenantSearchResult> listAllTenants() {
        List<Tenant> tenants = tenantRepository.findAll();
        Map<UUID, String> planNames = loadPlanNames();

        return tenants.stream()
                .map(t -> TenantSearchResult.builder()
                        .id(t.getId().toString())
                        .slug(t.getEmailContacto())
                        .nombre(t.getNombre())
                        .plan(planNames.getOrDefault(t.getPlanId(), "sin-plan"))
                        .estado(t.getEstado().name())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Search tenants by name or email (case-insensitive partial match).
     */
    public List<TenantSearchResult> searchTenants(String query) {
        String pattern = query.toLowerCase();
        List<Tenant> tenants = tenantRepository.findAll();
        Map<UUID, String> planNames = loadPlanNames();

        return tenants.stream()
                .filter(t -> t.getNombre().toLowerCase().contains(pattern)
                        || t.getEmailContacto().toLowerCase().contains(pattern))
                .limit(20)
                .map(t -> TenantSearchResult.builder()
                        .id(t.getId().toString())
                        .slug(t.getEmailContacto())
                        .nombre(t.getNombre())
                        .plan(planNames.getOrDefault(t.getPlanId(), "sin-plan"))
                        .estado(t.getEstado().name())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Get the credit balance summary for a tenant.
     */
    public TenantBalanceResponse getBalance(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));

        Map<UUID, String> planNames = loadPlanNames();

        // Get active package
        Optional<PaqueteCreditos> paqueteOpt = paqueteCreditosRepository
                .findByTenantIdAndEstado(tenantId, EstadoPaquete.ACTIVE);

        BigDecimal saldoTotal = BigDecimal.ZERO;
        int totalAdquiridos = 0;

        if (paqueteOpt.isPresent()) {
            PaqueteCreditos paquete = paqueteOpt.get();
            saldoTotal = paquete.getSaldoDisponible();
            totalAdquiridos = paquete.getCreditosTotalesAdquiridos();
        }

        int totalConsumidos = totalAdquiridos - saldoTotal.intValue();

        return TenantBalanceResponse.builder()
                .tenantId(tenantId.toString())
                .slug(tenant.getEmailContacto())
                .nombre(tenant.getNombre())
                .plan(planNames.getOrDefault(tenant.getPlanId(), "sin-plan"))
                .estado(tenant.getEstado().name())
                .saldoCreditos(saldoTotal)
                .creditosTotalesAdquiridos(totalAdquiridos)
                .creditosConsumidos(Math.max(0, totalConsumidos))
                .ultimoSync(Instant.now().toString())
                .build();
    }

    /**
     * Get paginated credit history for a tenant.
     * Note: Basic implementation using findAll + filter since repositories are minimal.
     */
    public PaginatedLedgerResponse getHistory(UUID tenantId, String tipo, String desde, String hasta, int page, int pageSize) {
        if (!tenantRepository.existsById(tenantId)) {
            throw new TenantNotFoundException(tenantId);
        }

        // Get all events and filter (repository doesn't have tenant-specific query methods)
        List<EventoCredito> allEventos = eventoCreditoRepository.findAll();

        List<EventoCredito> filtered = allEventos.stream()
                .filter(e -> e.getTenant() != null && tenantId.equals(e.getTenant().getId()))
                .filter(e -> tipo == null || tipo.isBlank() || e.getTipo().name().equalsIgnoreCase(tipo))
                .sorted(Comparator.comparing(EventoCredito::getOcurridoEn).reversed())
                .collect(Collectors.toList());

        long total = filtered.size();
        int totalPages = (int) Math.ceil((double) total / pageSize);
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());

        List<LedgerEntryResponse> entries = filtered.subList(fromIndex, toIndex).stream()
                .map(e -> LedgerEntryResponse.builder()
                        .id(e.getId().toString())
                        .tipo(e.getTipo().name().toLowerCase())
                        .cantidad(e.getCantidad())
                        .saldoResultante(e.getSaldoResultante())
                        .concepto(e.getTipo().name())
                        .perfilDocumento(e.getPerfilDocumento())
                        .referencia(e.getDocumentoId() != null ? e.getDocumentoId().toString() : null)
                        .actorId(e.getUsuarioId() != null ? e.getUsuarioId().toString() : null)
                        .actorEmail(null)
                        .createdAt(e.getOcurridoEn() != null ? e.getOcurridoEn().toString() : "")
                        .build())
                .collect(Collectors.toList());

        return PaginatedLedgerResponse.builder()
                .data(entries)
                .total(total)
                .page(page)
                .pageSize(pageSize)
                .totalPages(totalPages)
                .build();
    }

    /**
     * Adjust credits for a tenant. Updates the active package and publishes Kafka event.
     */
    @Transactional
    public AdjustCreditsResponse adjustCredits(UUID tenantId, AdjustCreditsRequest request, UUID actorId) {
        // 1. Validate tenant
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));

        // 1b. Tenant suspendido por impago: se bloquea el ajuste manual de cartera.
        //     Debe regularizar el pago mensual (SPEI) para reactivarse.
        if (tenant.getEstado() == com.mikelcrm.licenseservice.domain.enums.EstadoTenant.SUSPENDED) {
            return AdjustCreditsResponse.builder()
                    .status("rejected")
                    .message("Tenant suspendido por impago. Regularice el pago mensual para habilitar ajustes de cartera.")
                    .timestamp(Instant.now().toString())
                    .build();
        }

        // 2. Get active package
        PaqueteCreditos paquete = paqueteCreditosRepository
                .findByTenantIdAndEstado(tenantId, EstadoPaquete.ACTIVE)
                .orElse(null);

        if (paquete == null) {
            return AdjustCreditsResponse.builder()
                    .status("rejected")
                    .message("El tenant no tiene un paquete de créditos activo")
                    .timestamp(Instant.now().toString())
                    .build();
        }

        // 3. Calculate new balance
        BigDecimal adjustment = request.getCantidad();
        BigDecimal currentBalance = paquete.getSaldoDisponible();
        BigDecimal newBalance = currentBalance.add(adjustment);

        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            return AdjustCreditsResponse.builder()
                    .status("rejected")
                    .message("El ajuste resultaría en saldo negativo. Saldo actual: " + currentBalance)
                    .timestamp(Instant.now().toString())
                    .build();
        }

        // Validar tope máximo de cartera: 25,000 créditos
        if (newBalance.compareTo(BigDecimal.valueOf(25000)) > 0) {
            return AdjustCreditsResponse.builder()
                    .status("rejected")
                    .message("La cartera no puede exceder 25,000 créditos. Saldo actual: " + currentBalance)
                    .timestamp(Instant.now().toString())
                    .build();
        }

        // 4. Apply adjustment
        paquete.setSaldoDisponible(newBalance);
        paqueteCreditosRepository.save(paquete);

        // 5. Record evento_credito
        TipoEventoCredito tipoEvento = "recarga".equals(request.getTipo())
                ? TipoEventoCredito.COMPRA
                : TipoEventoCredito.COMPENSACION;

        EventoCredito evento = EventoCredito.builder()
                .id(UUID.randomUUID())
                .tenant(tenant)
                .paquete(paquete)
                .tipo(tipoEvento)
                .cantidad(adjustment)
                .saldoResultante(newBalance)
                .perfilDocumento(null)
                .documentoId(null)
                .usuarioId(actorId)
                .ocurridoEn(LocalDateTime.now())
                .build();
        eventoCreditoRepository.save(evento);

        // 6. Registrar ABONO en el estado de cuenta (fiscalizable).
        // Solo las recargas representan dinero real recibido del cliente
        // (Plan Mensual, Pago Estudio, Abono Prepago). La cartera puede incluir
        // otros créditos operativos/bonos que NO se reflejan aquí.
        if ("recarga".equals(request.getTipo()) && adjustment.compareTo(BigDecimal.ZERO) > 0) {
            estadoCuentaService.registrarAbono(
                    tenant,
                    adjustment,
                    request.getMotivo(),      // concepto: motivo del ajuste
                    request.getReferencia(),  // referencia de pago
                    request.getAutorizacion(),// clave de rastreo / autorización
                    null,                     // periodoMes (no aplica a ajuste manual)
                    null,                     // cobroMensualId (no aplica)
                    null                      // pagoSpeiId (no aplica)
            );
        }

        // 7. Publish Kafka event for SMT sync
        publishLedgerEvent(tenant, evento, newBalance);

        log.info("[CarteraAdmin] Ajuste aplicado: tenant={}, tipo={}, cantidad={}, nuevoSaldo={}, actor={}",
                tenant.getNombre(), request.getTipo(), adjustment, newBalance, actorId);

        return AdjustCreditsResponse.builder()
                .status("approved")
                .saldoResultante(newBalance)
                .eventoId(evento.getId().toString())
                .timestamp(Instant.now().toString())
                .build();
    }

    /**
     * Reconcilia el estado de cuenta de TODOS los tenants con sus movimientos
     * de cartera existentes. Idempotente: solo crea los abonos faltantes.
     * Pensado para ejecutarse automáticamente al arrancar el backend.
     *
     * @return número total de abonos creados en esta ejecución.
     */
    @Transactional
    public int reconciliarTodos() {
        List<Tenant> tenants = tenantRepository.findAll();
        int totalCreados = 0;
        for (Tenant tenant : tenants) {
            try {
                totalCreados += reconciliarEstadoCuenta(tenant.getId());
            } catch (Exception e) {
                log.error("[CarteraAdmin] Error reconciliando tenant {}: {}", tenant.getId(), e.getMessage());
            }
        }
        log.info("[CarteraAdmin] Reconciliación global completada: tenants={}, abonosCreados={}",
                tenants.size(), totalCreados);
        return totalCreados;
    }

    /**
     * Reconcilia el estado de cuenta con los movimientos de cartera existentes.
     *
     * Toma los eventos de crédito de tipo COMPRA (recargas = dinero real recibido)
     * del tenant y crea el ABONO correspondiente en el estado de cuenta si aún no
     * existe. Es idempotente: usa el ID del evento como clave de rastreo, por lo que
     * ejecutarlo varias veces no genera duplicados.
     *
     * Los eventos BONUS / COMPENSACION / CONSUMO NO se reconcilian porque no
     * representan dinero real fiscalizable.
     *
     * @return número de abonos creados en esta ejecución.
     */
    @Transactional
    public int reconciliarEstadoCuenta(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));

        // Eventos COMPRA del tenant, en orden cronológico ascendente para
        // que el saldo resultante del estado de cuenta se acumule correctamente.
        List<EventoCredito> compras = eventoCreditoRepository.findAll().stream()
                .filter(e -> e.getTenant() != null && tenantId.equals(e.getTenant().getId()))
                .filter(e -> e.getTipo() == TipoEventoCredito.COMPRA)
                .sorted(Comparator.comparing(EventoCredito::getOcurridoEn))
                .collect(Collectors.toList());

        int creados = 0;
        for (EventoCredito evento : compras) {
            String claveRastreo = "CARTERA-" + evento.getId();
            EstadoCuentaTenant mov = estadoCuentaService.registrarAbonoHistorico(
                    tenant,
                    evento.getCantidad(),
                    "Recarga de cartera (reconciliación)",
                    null,
                    claveRastreo,
                    evento.getOcurridoEn());
            if (mov != null) {
                creados++;
            }
        }

        log.info("[CarteraAdmin] Reconciliación de estado de cuenta: tenant={}, comprasEvaluadas={}, abonosCreados={}",
                tenant.getNombre(), compras.size(), creados);

        return creados;
    }

    // ─── Private helpers ──────────────────────────────────────────────────

    private Map<UUID, String> loadPlanNames() {
        return planRepository.findAll().stream()
                .collect(Collectors.toMap(Plan::getId, Plan::getNombre, (a, b) -> a));
    }

    private void publishLedgerEvent(Tenant tenant, EventoCredito evento, BigDecimal saldoResultante) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "credit.ledger.entry");
        payload.put("tenant_id", tenant.getId().toString());
        payload.put("nombre", tenant.getNombre());

        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("id", evento.getId().toString());
        entry.put("tipo", evento.getTipo().name().toLowerCase());
        entry.put("cantidad", evento.getCantidad());
        entry.put("saldo_resultante", saldoResultante.doubleValue());
        entry.put("perfil_documento", evento.getPerfilDocumento());
        entry.put("referencia", evento.getDocumentoId() != null ? evento.getDocumentoId().toString() : null);
        payload.put("entry", entry);

        payload.put("timestamp", Instant.now().toString());

        domainEventPublisher.publishRawToTopic(KAFKA_TOPIC, tenant.getId(), payload);
    }
}
