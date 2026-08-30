package com.mikelcrm.licenseservice.security.rbac;

import com.mikelcrm.licenseservice.event.DomainEventPublisher;
import com.mikelcrm.licenseservice.security.TenantAuthenticationToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Aspect that ensures tenant confinement — verifies that the tenantId from
 * the JWT matches the tenantId path variable in every endpoint.
 * Prevents cross-tenant access with HTTP 403 "cross_tenant_access".
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class TenantConfinementAspect {

    private final DomainEventPublisher eventPublisher;

    /**
     * Intercepts all controller methods annotated with Spring MVC request mapping
     * annotations that have a UUID parameter named "tenantId" annotated with @PathVariable.
     */
    @Around("execution(* com.mikelcrm.licenseservice.controller..*(..)) && args(.., java.util.UUID, ..)")
    public Object enforceConfinement(ProceedingJoinPoint joinPoint) throws Throwable {
        UUID pathTenantId = extractTenantIdFromArgs(joinPoint);
        if (pathTenantId == null) {
            // No tenantId in path, skip confinement check
            return joinPoint.proceed();
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof TenantAuthenticationToken tenantAuth)) {
            return joinPoint.proceed();
        }

        // platform_admin administra todos los tenants — se exceptúa del confinamiento
        if ("platform_admin".equals(tenantAuth.getRol())) {
            return joinPoint.proceed();
        }

        UUID jwtTenantId = tenantAuth.getTenantId();

        if (!jwtTenantId.equals(pathTenantId)) {
            log.warn("Cross-tenant access attempt: JWT tenantId={}, path tenantId={}, user={}",
                    jwtTenantId, pathTenantId, tenantAuth.getUserId());

            publishCrossTenantEvent(tenantAuth, pathTenantId);

            Map<String, String> errorBody = new HashMap<>();
            errorBody.put("error", "cross_tenant_access");
            errorBody.put("motivo", "tenantId mismatch between JWT and path");

            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorBody);
        }

        return joinPoint.proceed();
    }

    private UUID extractTenantIdFromArgs(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            PathVariable pathVariable = param.getAnnotation(PathVariable.class);
            if (pathVariable != null && args[i] instanceof UUID uuid) {
                String name = pathVariable.value().isEmpty() ? param.getName() : pathVariable.value();
                if ("tenantId".equals(name)) {
                    return uuid;
                }
            }
            // Also check by parameter name when annotation value is empty
            if (pathVariable != null && param.getName().equals("tenantId") && args[i] instanceof UUID uuid) {
                return uuid;
            }
        }

        return null;
    }

    private void publishCrossTenantEvent(TenantAuthenticationToken auth, UUID targetTenantId) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("tenantId", auth.getTenantId().toString());
            payload.put("userId", auth.getUserId().toString());
            payload.put("targetTenantId", targetTenantId.toString());
            payload.put("motivo", "cross_tenant_access");

            eventPublisher.publish("access.denied", auth.getTenantId(), payload, UUID.randomUUID().toString());
        } catch (Exception e) {
            log.error("Failed to publish cross-tenant access.denied event: {}", e.getMessage());
        }
    }
}
