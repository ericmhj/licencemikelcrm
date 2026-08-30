package com.mikelcrm.licenseservice.controller;

import com.mikelcrm.licenseservice.domain.entity.EstadoCuentaTenant;
import com.mikelcrm.licenseservice.domain.entity.Tenant;
import com.mikelcrm.licenseservice.domain.enums.TipoMovimientoEdoCuenta;
import com.mikelcrm.licenseservice.domain.repository.EstadoCuentaTenantRepository;
import com.mikelcrm.licenseservice.domain.repository.TenantRepository;
import com.mikelcrm.licenseservice.exception.TenantNotFoundException;
import com.mikelcrm.licenseservice.security.TenantAuthenticationToken;
import com.mikelcrm.licenseservice.service.command.EstadoCuentaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/estado-cuenta")
@RequiredArgsConstructor
@Slf4j
public class EstadoCuentaController {

    private final EstadoCuentaTenantRepository estadoCuentaRepository;
    private final TenantRepository tenantRepository;
    private final EstadoCuentaService estadoCuentaService;
    private final com.mikelcrm.licenseservice.domain.repository.PlanRepository planRepository;

    private static final String PLATFORM_ADMIN_ROLE = "platform_admin";

    /**
     * GET /api/v1/tenants/{tenantId}/estado-cuenta/resumen
     * Returns summary: saldo actual, total abonos, total cargos.
     */
    @GetMapping("/resumen")
    public ResponseEntity<?> getResumen(@PathVariable UUID tenantId, Authentication auth) {
        if (!isPlatformAdmin(auth)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "NOT_FOUND", "message", "Liga desconocida"));
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));

        BigDecimal saldoActual = estadoCuentaService.obtenerSaldoActual(tenantId);

        // Calculate totals
        List<EstadoCuentaTenant> todos = estadoCuentaRepository
                .findByTenantIdOrderByRegistradoEnDesc(tenantId, PageRequest.of(0, 10000))
                .getContent();

        BigDecimal totalAbonos = todos.stream()
                .filter(m -> m.getTipo() == TipoMovimientoEdoCuenta.ABONO)
                .map(EstadoCuentaTenant::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCargos = todos.stream()
                .filter(m -> m.getTipo() == TipoMovimientoEdoCuenta.CARGO)
                .map(m -> m.getMonto().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Adeudo de la mensualidad del mes en curso (dato calculado, NO persistido:
        // el estado de cuenta solo registra dinero real. El adeudo se computa
        // comparando servicioPagadoHasta contra el mes actual).
        java.time.LocalDate mesActual = java.time.LocalDate.now().withDayOfMonth(1);
        boolean mesPagado = tenant.getServicioPagadoHasta() != null
                && !tenant.getServicioPagadoHasta().isBefore(mesActual);
        BigDecimal montoMensual = BigDecimal.ZERO;
        if (tenant.getPlanId() != null) {
            montoMensual = planRepository.findById(tenant.getPlanId())
                    .map(p -> p.getPrecioMensual())
                    .orElse(BigDecimal.ZERO);
        }
        BigDecimal adeudoMes = mesPagado ? BigDecimal.ZERO : montoMensual;

        String planNombre = tenant.getPlanId() != null
                ? planRepository.findById(tenant.getPlanId()).map(p -> p.getNombre()).orElse("sin-plan")
                : "sin-plan";

        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("tenantId", tenantId.toString());
        resumen.put("nombre", tenant.getNombre());
        // Encabezado: plan y estado del tenant
        resumen.put("plan", planNombre);
        resumen.put("estado", tenant.getEstado().name());
        resumen.put("saldoActual", saldoActual);
        resumen.put("totalAbonos", totalAbonos);
        resumen.put("totalCargos", totalCargos);
        // Adeudo de mensualidad
        resumen.put("mensualidad", montoMensual);
        resumen.put("mesPagado", mesPagado);
        resumen.put("adeudoMes", adeudoMes);
        resumen.put("periodoMes", mesActual.toString());
        resumen.put("servicioPagadoHasta",
                tenant.getServicioPagadoHasta() != null ? tenant.getServicioPagadoHasta().toString() : null);

        return ResponseEntity.ok(resumen);
    }

    /**
     * GET /api/v1/tenants/{tenantId}/estado-cuenta?tipo=ABONO&page=1&pageSize=20
     * Returns paginated movements.
     */
    @GetMapping
    public ResponseEntity<?> getMovimientos(
            @PathVariable UUID tenantId,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            Authentication auth) {

        if (!isPlatformAdmin(auth)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "NOT_FOUND", "message", "Liga desconocida"));
        }

        if (!tenantRepository.existsById(tenantId)) {
            throw new TenantNotFoundException(tenantId);
        }

        // Rango de fechas (opcional): "YYYY-MM-DD"
        final java.time.LocalDateTime desdeDt = (desde != null && !desde.isBlank())
                ? java.time.LocalDate.parse(desde).atStartOfDay() : null;
        final java.time.LocalDateTime hastaDt = (hasta != null && !hasta.isBlank())
                ? java.time.LocalDate.parse(hasta).atTime(23, 59, 59) : null;

        pageSize = Math.min(pageSize, 100);
        Page<EstadoCuentaTenant> movPage = estadoCuentaRepository
                .findByTenantIdOrderByRegistradoEnDesc(tenantId, PageRequest.of(page - 1, pageSize));

        List<Map<String, Object>> data = movPage.getContent().stream()
                .filter(m -> tipo == null || tipo.isBlank() || m.getTipo().name().equals(tipo))
                .filter(m -> desdeDt == null || !m.getRegistradoEn().isBefore(desdeDt))
                .filter(m -> hastaDt == null || !m.getRegistradoEn().isAfter(hastaDt))
                .map(m -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("id", m.getId().toString());
                    entry.put("tipo", m.getTipo().name());
                    entry.put("monto", m.getMonto());
                    entry.put("saldoResultante", m.getSaldoResultante());
                    entry.put("concepto", m.getConcepto());
                    entry.put("referencia", m.getReferencia());
                    entry.put("claveRastreo", m.getClaveRastreo());
                    entry.put("periodoMes", m.getPeriodoMes());
                    entry.put("registradoEn", m.getRegistradoEn().toString());
                    return entry;
                })
                .collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("data", data);
        response.put("total", movPage.getTotalElements());
        response.put("page", page);
        response.put("pageSize", pageSize);
        response.put("totalPages", movPage.getTotalPages());

        return ResponseEntity.ok(response);
    }

    private boolean isPlatformAdmin(Authentication auth) {
        if (auth instanceof TenantAuthenticationToken tenantAuth) {
            return PLATFORM_ADMIN_ROLE.equals(tenantAuth.getRol());
        }
        return false;
    }
}
