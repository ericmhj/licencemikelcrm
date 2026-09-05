package com.mikelcrm.licenseservice.controller;

import com.mikelcrm.licenseservice.controller.dto.ProvisioningInfoResponse;
import com.mikelcrm.licenseservice.domain.entity.Tenant;
import com.mikelcrm.licenseservice.domain.repository.PlanRepository;
import com.mikelcrm.licenseservice.domain.repository.TenantRepository;
import com.mikelcrm.licenseservice.security.TenantAuthenticationToken;
import com.mikelcrm.licenseservice.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import com.mikelcrm.licenseservice.domain.enums.EstadoTenant;
import com.mikelcrm.licenseservice.exception.TenantNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Endpoint de RECONCILIACIÓN server-to-server.
 * <p>
 * Expone los datos de materialización de todos los tenants (fuente de verdad)
 * para que SMT reconcilie de forma masiva los que falten en su propio sistema,
 * sin depender de Kafka. Pensado para llamadas servicio-a-servicio autenticadas
 * por el gateway header (X-User-Role=platform_admin + X-Gateway-Secret).
 */
@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
@Slf4j
public class ProvisioningInfoController {

    private final TenantRepository tenantRepository;
    private final PlanRepository planRepository;

    private static final String PLATFORM_ADMIN_ROLE = "platform_admin";

    /**
     * GET /api/v1/tenants/provisioning-info
     * Devuelve la lista de todos los tenants con los datos necesarios para que
     * SMT los materialice. Solo accesible por platform_admin (o gateway header).
     */
    @GetMapping("/provisioning-info")
    public ResponseEntity<?> listProvisioningInfo(Authentication auth) {
        if (!isPlatformAdmin(auth)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "NOT_FOUND", "message", "Liga desconocida"));
        }

        List<ProvisioningInfoResponse> data = tenantRepository.findAll().stream()
                .map(this::toProvisioningInfo)
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("data", data, "total", data.size()));
    }

    /**
     * GET /api/v1/tenants/{tenantId}/state
     * <p>
     * Endpoint server-to-server para que SMT valide, en el login, el estado
     * EFECTIVO del tenant (fuente de verdad). Devuelve el mismo criterio que la
     * vista de planes/estados: {@code getEstadoEfectivo()}, que trata como
     * SUSPENDED a un tenant ACTIVE cuya renta mensual no está al corriente,
     * aunque el job diario aún no lo haya persistido.
     * <p>
     * Autenticado por gateway header (platform_admin + X-Gateway-Secret). No
     * requiere userId, a diferencia de {@code /access/{tenantId}}.
     */
    @GetMapping("/{tenantId}/state")
    public ResponseEntity<?> getTenantState(@PathVariable UUID tenantId, Authentication auth) {
        if (!isPlatformAdmin(auth)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "NOT_FOUND", "message", "Liga desconocida"));
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));

        EstadoTenant efectivo = tenant.getEstadoEfectivo();
        boolean active = efectivo == EstadoTenant.ACTIVE;

        return ResponseEntity.ok(Map.of(
                "tenantId", tenant.getId().toString(),
                "status", efectivo.name(),
                "active", active
        ));
    }

    private ProvisioningInfoResponse toProvisioningInfo(Tenant tenant) {
        String planCodigo = null;
        if (tenant.getPlanId() != null) {
            planCodigo = planRepository.findById(tenant.getPlanId())
                    .map(p -> p.getCodigo())
                    .orElse(null);
        }

        return ProvisioningInfoResponse.builder()
                .tenantId(tenant.getId().toString())
                .slug(SlugUtil.toSlug(tenant.getNombre()))
                .nombre(tenant.getNombre())
                .planCodigo(planCodigo)
                .estado(tenant.getEstado().name())
                .adminEmail(tenant.getEmailContacto())
                .build();
    }

    private boolean isPlatformAdmin(Authentication auth) {
        if (auth instanceof TenantAuthenticationToken tenantAuth) {
            return PLATFORM_ADMIN_ROLE.equals(tenantAuth.getRol());
        }
        return false;
    }
}
