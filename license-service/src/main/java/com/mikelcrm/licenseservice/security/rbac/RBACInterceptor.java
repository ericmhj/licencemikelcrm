package com.mikelcrm.licenseservice.security.rbac;

import com.mikelcrm.licenseservice.audit.AuditService;
import com.mikelcrm.licenseservice.domain.enums.RolTenant;
import com.mikelcrm.licenseservice.event.DomainEventPublisher;
import com.mikelcrm.licenseservice.security.TenantAuthenticationToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Spring MVC interceptor that evaluates RBAC permissions before handler execution.
 * Checks the @RequiresPermission annotation on controller methods against the
 * authenticated user's role via the PermissionMatrix.
 * Also denies access if the tenant's module is suspended.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RBACInterceptor implements HandlerInterceptor {

    private final PermissionMatrix permissionMatrix;
    private final DomainEventPublisher eventPublisher;
    private final AuditService auditService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Only process annotated handler methods
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequiresPermission annotation = handlerMethod.getMethodAnnotation(RequiresPermission.class);
        if (annotation == null) {
            return true;
        }

        // Get authentication from SecurityContext
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof TenantAuthenticationToken tenantAuth)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"access_denied\",\"motivo\":\"no_authentication\"}");
            return false;
        }

        String rolStr = tenantAuth.getRol();
        RolTenant rol;
        try {
            rol = RolTenant.valueOf(rolStr);
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"access_denied\",\"motivo\":\"invalid_role\"}");
            return false;
        }

        String recurso = annotation.recurso();
        String accion = annotation.accion();

        // Evaluate permission
        if (!permissionMatrix.isAllowed(rol, recurso, accion)) {
            log.warn("Access denied for user {} (role {}) on resource={} action={}",
                    tenantAuth.getUserId(), rol, recurso, accion);

            publishAccessDeniedEvent(tenantAuth, recurso, accion, "permission_denied");

            // Register denied access in audit log (Task 13.2)
            String ipOrigen = extractIpAddress(request);
            auditService.registrarAccesoDenegado(
                    tenantAuth.getTenantId(), tenantAuth.getUserId(),
                    accion, recurso, "permission_denied", ipOrigen);

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"access_denied\",\"motivo\":\"permission_denied\",\"recurso\":\"" + recurso + "\",\"accion\":\"" + accion + "\"}");
            return false;
        }

        return true;
    }

    private String extractIpAddress(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void publishAccessDeniedEvent(TenantAuthenticationToken auth, String recurso, String accion, String motivo) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("tenantId", auth.getTenantId().toString());
            payload.put("userId", auth.getUserId().toString());
            payload.put("recurso", recurso);
            payload.put("accion", accion);
            payload.put("motivo", motivo);
            payload.put("rol", auth.getRol());

            eventPublisher.publish("access.denied", auth.getTenantId(), payload, UUID.randomUUID().toString());
        } catch (Exception e) {
            log.error("Failed to publish access.denied event: {}", e.getMessage());
        }
    }
}
