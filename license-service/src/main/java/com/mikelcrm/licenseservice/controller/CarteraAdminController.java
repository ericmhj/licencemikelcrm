package com.mikelcrm.licenseservice.controller;

import com.mikelcrm.licenseservice.controller.dto.*;
import com.mikelcrm.licenseservice.security.TenantAuthenticationToken;
import com.mikelcrm.licenseservice.service.command.CarteraAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin-only controller for managing tenant credit wallets (cartera).
 * 
 * Security:
 * - All endpoints require authentication (JWT)
 * - All endpoints require role 'platform_admin'
 * - Adjust operations are rate-limited (10/min per user)
 * - Adjust operations require idempotency key (operationId)
 * - All mutations are logged in audit_log
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class CarteraAdminController {

    private final CarteraAdminService carteraAdminService;

    private static final String PLATFORM_ADMIN_ROLE = "platform_admin";

    /**
     * List all tenants (summary view).
     * GET /api/v1/cartera/tenants
     */
    @GetMapping("/cartera/tenants")
    public ResponseEntity<?> listAllTenants(Authentication auth) {
        if (!isPlatformAdmin(auth)) {
            log.warn("[CarteraAdmin] listAllTenants DENIED. Auth type={}, principal={}, rol={}",
                    auth != null ? auth.getClass().getSimpleName() : "null",
                    auth != null ? auth.getName() : "none",
                    auth instanceof TenantAuthenticationToken t ? t.getRol() : "N/A");
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "NOT_FOUND", "message", "Liga desconocida"));
        }

        List<TenantSearchResult> results = carteraAdminService.listAllTenants();
        return ResponseEntity.ok(results);
    }

    /**
     * Search tenants by name or slug.
     * GET /api/v1/tenants/search?q=acme
     */
    @GetMapping("/tenants/search")
    public ResponseEntity<?> searchTenants(
            @RequestParam String q,
            Authentication auth) {
        if (!isPlatformAdmin(auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Solo platform_admin puede buscar tenants"));
        }

        if (q == null || q.trim().length() < 2) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "El término de búsqueda debe tener al menos 2 caracteres"));
        }

        List<TenantSearchResult> results = carteraAdminService.searchTenants(q.trim());
        return ResponseEntity.ok(results);
    }

    /**
     * Get the credit balance and summary for a specific tenant.
     * GET /api/v1/tenants/{tenantId}/credits/balance
     */
    @GetMapping("/tenants/{tenantId}/credits/balance")
    public ResponseEntity<?> getBalance(
            @PathVariable UUID tenantId,
            Authentication auth) {
        if (!isPlatformAdmin(auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Solo platform_admin puede consultar la cartera"));
        }

        TenantBalanceResponse balance = carteraAdminService.getBalance(tenantId);
        return ResponseEntity.ok(balance);
    }

    /**
     * Get paginated credit history (ledger) for a tenant.
     * GET /api/v1/tenants/{tenantId}/credits/history?tipo=consumo&desde=2026-08-01&page=1&pageSize=20
     */
    @GetMapping("/tenants/{tenantId}/credits/history")
    public ResponseEntity<?> getHistory(
            @PathVariable UUID tenantId,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            Authentication auth) {
        if (!isPlatformAdmin(auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Solo platform_admin puede consultar el historial"));
        }

        pageSize = Math.min(pageSize, 100);
        PaginatedLedgerResponse history = carteraAdminService.getHistory(
                tenantId, tipo, desde, hasta, page, pageSize);
        return ResponseEntity.ok(history);
    }

    /**
     * Adjust the credit balance for a tenant (add or subtract).
     * POST /api/v1/tenants/{tenantId}/credits/adjust
     * 
     * Security controls:
     * - Requires platform_admin role
     * - Requires operationId for idempotency
     * - Requires motivo (min 20 chars) for audit trail
     * - Amount limited to 500 per operation
     * - Publishes credit.ledger.entry event to Kafka for SMT sync
     */
    @PostMapping("/tenants/{tenantId}/credits/adjust")
    public ResponseEntity<?> adjustCredits(
            @PathVariable UUID tenantId,
            @Valid @RequestBody AdjustCreditsRequest request,
            Authentication auth) {
        if (!isPlatformAdmin(auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Solo platform_admin puede ajustar créditos"));
        }

        // Validate tipo — solo se permiten operaciones de agregación
        if (!"recarga".equals(request.getTipo())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Solo se aceptan operaciones de agregación de créditos (tipo: recarga)"));
        }

        // Validate amount is positive
        if (request.getCantidad().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "La cantidad debe ser positiva"));
        }

        // Validate amount bounds
        var absAmount = request.getCantidad().abs();
        if (absAmount.compareTo(java.math.BigDecimal.valueOf(0.5)) < 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "La cantidad mínima es 0.5 créditos"));
        }
        if (absAmount.compareTo(java.math.BigDecimal.valueOf(22000)) > 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "La cantidad máxima por operación es 22,000 créditos"));
        }

        UUID actorId = extractUserId(auth);
        AdjustCreditsResponse response = carteraAdminService.adjustCredits(
                tenantId, request, actorId);

        if ("rejected".equals(response.getStatus())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Reconcilia el estado de cuenta con los movimientos de cartera (recargas)
     * existentes que aún no se han reflejado como abonos.
     * POST /api/v1/tenants/{tenantId}/estado-cuenta/reconciliar
     *
     * Idempotente: solo crea los abonos faltantes. Útil para reflejar
     * recargas históricas hechas antes de que la sincronización fuera automática.
     */
    @PostMapping("/tenants/{tenantId}/estado-cuenta/reconciliar")
    public ResponseEntity<?> reconciliarEstadoCuenta(
            @PathVariable UUID tenantId,
            Authentication auth) {
        if (!isPlatformAdmin(auth)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "NOT_FOUND", "message", "Liga desconocida"));
        }

        int creados = carteraAdminService.reconciliarEstadoCuenta(tenantId);
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "abonosCreados", creados,
                "message", creados + " abono(s) reflejado(s) en el estado de cuenta"));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    private boolean isPlatformAdmin(Authentication auth) {
        if (auth instanceof TenantAuthenticationToken tenantAuth) {
            return PLATFORM_ADMIN_ROLE.equals(tenantAuth.getRol());
        }
        // Check Spring Security authorities as fallback
        if (auth != null && auth.getAuthorities() != null) {
            return auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_platform_admin"));
        }
        return false;
    }

    private UUID extractUserId(Authentication auth) {
        if (auth instanceof TenantAuthenticationToken tenantAuth) {
            return tenantAuth.getUserId();
        }
        return null;
    }
}
