package com.mikelcrm.licenseservice.controller;

import com.mikelcrm.licenseservice.domain.entity.CuentaClabeTenant;
import com.mikelcrm.licenseservice.domain.entity.PagoSpeiRecibido;
import com.mikelcrm.licenseservice.domain.enums.TipoCobro;
import com.mikelcrm.licenseservice.service.command.ClabeAdminService;
import com.mikelcrm.licenseservice.service.command.PagoManualService;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Endpoints de administración de cobros para el portal:
 *  - CRUD de CLABEs por tenant.
 *  - Registro manual de pago mensual (sin Stripe).
 *
 * Requiere autenticación platform_admin (confinamiento por tenant exento).
 */
@RestController
@RequestMapping("/api/v1/tenants/{tenantId}")
@RequiredArgsConstructor
public class PagoAdminController {

    private final ClabeAdminService clabeAdminService;
    private final PagoManualService pagoManualService;

    // ─── CLABEs ───────────────────────────────────────────────────────────

    @GetMapping("/clabes")
    public ResponseEntity<List<Map<String, Object>>> listClabes(@PathVariable UUID tenantId) {
        List<Map<String, Object>> data = clabeAdminService.listByTenant(tenantId).stream()
                .map(this::toClabeDto)
                .toList();
        return ResponseEntity.ok(data);
    }

    @PostMapping("/clabes")
    public ResponseEntity<Map<String, Object>> createClabe(
            @PathVariable UUID tenantId,
            @RequestBody CreateClabeRequest request) {
        CuentaClabeTenant clabe = clabeAdminService.createClabe(
                tenantId, request.clabe(), request.tipo(), request.psp());
        return ResponseEntity.status(HttpStatus.CREATED).body(toClabeDto(clabe));
    }

    @DeleteMapping("/clabes/{clabeId}")
    public ResponseEntity<Void> deactivateClabe(
            @PathVariable UUID tenantId,
            @PathVariable UUID clabeId) {
        clabeAdminService.deactivateClabe(clabeId);
        return ResponseEntity.noContent().build();
    }

    // ─── Pago manual ──────────────────────────────────────────────────────

    @PostMapping("/pagos/mensual")
    public ResponseEntity<Map<String, Object>> registrarPagoMensual(
            @PathVariable UUID tenantId,
            @RequestBody RegistrarPagoRequest request) {
        PagoSpeiRecibido pago = pagoManualService.registrarPagoMensual(
                tenantId, request.monto(), request.claveRastreo(), request.referencia());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", pago.getId() != null ? pago.getId().toString() : null);
        body.put("estado", pago.getEstado() != null ? pago.getEstado().name() : null);
        body.put("monto", pago.getMonto());
        body.put("creditosOtorgados", pago.getCreditosOtorgados());
        body.put("claveRastreo", pago.getClaveRastreo());
        HttpStatus status = "APLICADO".equals(body.get("estado"))
                ? HttpStatus.OK : HttpStatus.UNPROCESSABLE_ENTITY;
        return ResponseEntity.status(status).body(body);
    }

    // ─── Helpers / DTOs ───────────────────────────────────────────────────

    private Map<String, Object> toClabeDto(CuentaClabeTenant c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId().toString());
        m.put("clabe", c.getClabe());
        m.put("tipo", c.getTipo().name());
        m.put("psp", c.getPsp());
        m.put("activa", c.getActiva());
        m.put("creadaEn", c.getCreadaEn() != null ? c.getCreadaEn().toString() : null);
        return m;
    }

    public record CreateClabeRequest(
            @NotNull String clabe,
            @NotNull TipoCobro tipo,
            String psp) {}

    public record RegistrarPagoRequest(
            @NotNull @Positive BigDecimal monto,
            String claveRastreo,
            String referencia) {}
}
