package com.mikelcrm.licenseservice.security.rbac;

import com.mikelcrm.licenseservice.domain.enums.RolTenant;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * In-memory permission matrix defining allowed and denied actions per role.
 * Deny rules take precedence over allow rules.
 * Format: Map<RolTenant, Map<String recurso, Set<String acciones>>>
 */
@Component
public class PermissionMatrix {

    private static final Map<RolTenant, Map<String, Set<String>>> PERMISSIONS = Map.of(
            RolTenant.admin, Map.of(
                    "contratos", Set.of("leer", "crear", "actualizar", "eliminar"),
                    "licencias", Set.of("leer", "crear", "actualizar"),
                    "creditos", Set.of("leer", "crear"),
                    "usuarios", Set.of("leer", "crear", "actualizar", "eliminar"),
                    "reportes", Set.of("leer"),
                    "tickets", Set.of("leer"),
                    "planes", Set.of("leer", "crear", "actualizar", "eliminar")
            ),
            RolTenant.manager, Map.of(
                    "tickets", Set.of("leer", "crear", "actualizar", "eliminar", "aprobar"),
                    "reportes", Set.of("leer", "crear"),
                    "clientes", Set.of("leer", "crear", "actualizar"),
                    "aprobacion_docs", Set.of("leer", "crear", "aprobar", "firmar")
            ),
            RolTenant.tecnico, Map.of(
                    "reportes", Set.of("leer", "crear"),
                    "creditos", Set.of("leer"),
                    "tickets", Set.of("leer", "actualizar")
            ),
            RolTenant.asistente, Map.of(
                    "clientes", Set.of("leer", "crear", "actualizar"),
                    "tickets", Set.of("leer", "crear", "actualizar"),
                    "reportes", Set.of("leer")
            )
    );

    // Explicit denies take precedence over allows
    private static final Map<RolTenant, Map<String, Set<String>>> DENIES = Map.of(
            RolTenant.asistente, Map.of(
                    "config_sistema", Set.of("leer", "crear", "actualizar", "eliminar", "configurar"),
                    "aprobacion_docs", Set.of("leer", "crear", "aprobar", "firmar"),
                    "creditos", Set.of("leer", "crear", "actualizar"),
                    "licencias", Set.of("leer", "crear", "actualizar")
            )
    );

    /**
     * Checks whether a given role is allowed to perform an action on a resource.
     * Deny rules are checked first and take precedence.
     *
     * @param rol     the tenant role
     * @param recurso the resource being accessed
     * @param accion  the action being performed
     * @return true if the action is allowed, false otherwise
     */
    public boolean isAllowed(RolTenant rol, String recurso, String accion) {
        // platform_admin administra la plataforma completa: acceso total.
        // (coherente con el bypass de platform_admin en TenantConfinementAspect).
        if (rol == RolTenant.platform_admin) {
            return true;
        }

        // Check explicit deny first (deny takes precedence)
        Map<String, Set<String>> rolDenies = DENIES.getOrDefault(rol, Map.of());
        Set<String> deniedActions = rolDenies.getOrDefault(recurso, Set.of());
        if (deniedActions.contains(accion)) {
            return false;
        }

        // Check allow
        Map<String, Set<String>> rolPerms = PERMISSIONS.getOrDefault(rol, Map.of());
        Set<String> allowedActions = rolPerms.getOrDefault(recurso, Set.of());
        return allowedActions.contains(accion);
    }

    /**
     * Returns the set of allowed actions for a given role and resource.
     */
    public Set<String> getAllowedActions(RolTenant rol, String recurso) {
        Map<String, Set<String>> rolPerms = PERMISSIONS.getOrDefault(rol, Map.of());
        return rolPerms.getOrDefault(recurso, Set.of());
    }
}
