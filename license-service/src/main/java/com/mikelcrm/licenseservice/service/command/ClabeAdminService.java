package com.mikelcrm.licenseservice.service.command;

import com.mikelcrm.licenseservice.domain.entity.CuentaClabeTenant;
import com.mikelcrm.licenseservice.domain.entity.Tenant;
import com.mikelcrm.licenseservice.domain.enums.TipoCobro;
import com.mikelcrm.licenseservice.domain.repository.CuentaClabeTenantRepository;
import com.mikelcrm.licenseservice.domain.repository.TenantRepository;
import com.mikelcrm.licenseservice.exception.TenantNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Administración de CLABEs SPEI por tenant.
 * Permite registrar, listar y desactivar las cuentas CLABE que identifican al
 * tenant cuando llega un pago (FIJO_MENSUAL o VARIABLE_PREPAGO).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClabeAdminService {

    private final CuentaClabeTenantRepository clabeRepository;
    private final TenantRepository tenantRepository;

    @Transactional(readOnly = true)
    public List<CuentaClabeTenant> listByTenant(UUID tenantId) {
        return clabeRepository.findByTenantIdAndActivaTrue(tenantId);
    }

    /**
     * Registra una nueva CLABE para el tenant. La CLABE debe ser única globalmente.
     * Si ya existe una CLABE activa del mismo tipo, se desactiva antes de crear la nueva.
     */
    @Transactional
    public CuentaClabeTenant createClabe(UUID tenantId, String clabe, TipoCobro tipo, String psp) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));

        String clabeNormalizada = clabe == null ? "" : clabe.trim();
        if (clabeNormalizada.length() != 18 || !clabeNormalizada.matches("\\d{18}")) {
            throw new IllegalArgumentException("La CLABE debe tener exactamente 18 dígitos numéricos");
        }

        // Unicidad global
        clabeRepository.findByClabe(clabeNormalizada).ifPresent(existing -> {
            throw new IllegalArgumentException("La CLABE " + clabeNormalizada + " ya está registrada");
        });

        // Regla: un tenant solo puede tener UNA CLABE activa. Cualquier CLABE
        // activa previa (sin importar el tipo) se desactiva antes de crear la nueva.
        clabeRepository.findByTenantIdAndActivaTrue(tenantId)
                .forEach(previa -> {
                    previa.setActiva(false);
                    clabeRepository.save(previa);
                    log.info("[Clabe] Desactivada CLABE previa {} (tipo {}) del tenant {}",
                            previa.getClabe(), previa.getTipo(), tenantId);
                });

        CuentaClabeTenant nueva = CuentaClabeTenant.builder()
                .tenant(tenant)
                .clabe(clabeNormalizada)
                .tipo(tipo)
                .psp(psp != null && !psp.isBlank() ? psp : "MANUAL")
                .activa(true)
                .build();

        nueva = clabeRepository.save(nueva);
        log.info("[Clabe] Registrada CLABE {} (tipo {}) para tenant {}", clabeNormalizada, tipo, tenantId);
        return nueva;
    }

    @Transactional
    public void deactivateClabe(UUID clabeId) {
        CuentaClabeTenant clabe = clabeRepository.findById(clabeId)
                .orElseThrow(() -> new IllegalArgumentException("CLABE no encontrada: " + clabeId));
        clabe.setActiva(false);
        clabeRepository.save(clabe);
        log.info("[Clabe] Desactivada CLABE {} (id {})", clabe.getClabe(), clabeId);
    }
}
