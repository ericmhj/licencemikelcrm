package com.mikelcrm.licenseservice.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mikelcrm.licenseservice.domain.entity.AuditLog;
import com.mikelcrm.licenseservice.domain.enums.ResultadoAudit;
import com.mikelcrm.licenseservice.domain.repository.AuditLogRepository;
import com.mikelcrm.licenseservice.security.TenantAuthenticationToken;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Parameter;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AOP aspect that intercepts methods annotated with @AuditAction and creates
 * immutable audit log entries. Writes are asynchronous to avoid blocking the response.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Around("@annotation(auditAction)")
    public Object auditMethod(ProceedingJoinPoint joinPoint, AuditAction auditAction) throws Throwable {
        // Capture context before execution
        UUID tenantId = extractTenantId(joinPoint);
        UUID userId = extractUserId();
        String ipOrigen = extractIpAddress();
        String payloadAntes = serializeArgs(joinPoint);

        Object result;
        ResultadoAudit resultado;

        try {
            result = joinPoint.proceed();
            resultado = ResultadoAudit.OK;
        } catch (Exception e) {
            resultado = ResultadoAudit.ERROR;
            // Save audit entry even on error
            saveAuditLogAsync(tenantId, userId, auditAction.accion(), auditAction.entidad(),
                    null, payloadAntes, null, resultado, ipOrigen);
            throw e;
        }

        // Capture "after" state
        String payloadDespues = serializeResult(result);
        UUID entidadId = extractEntityId(result);

        saveAuditLogAsync(tenantId, userId, auditAction.accion(), auditAction.entidad(),
                entidadId, payloadAntes, payloadDespues, resultado, ipOrigen);

        return result;
    }

    @Async
    void saveAuditLogAsync(UUID tenantId, UUID userId, String accion, String entidad,
                           UUID entidadId, String payloadAntes, String payloadDespues,
                           ResultadoAudit resultado, String ipOrigen) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .id(UUID.randomUUID())
                    .tenantId(tenantId)
                    .usuarioId(userId)
                    .accion(accion)
                    .entidad(entidad)
                    .entidadId(entidadId)
                    .payloadAntes(payloadAntes)
                    .payloadDespues(payloadDespues)
                    .resultado(resultado)
                    .ipOrigen(ipOrigen)
                    .ocurridoEn(LocalDateTime.now())
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log saved: accion={}, entidad={}, tenantId={}", accion, entidad, tenantId);
        } catch (Exception e) {
            log.error("Failed to save audit log: accion={}, entidad={}, error={}", accion, entidad, e.getMessage());
        }
    }

    private UUID extractTenantId(ProceedingJoinPoint joinPoint) {
        // First try to get from SecurityContext
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof TenantAuthenticationToken tenantAuth) {
            return tenantAuth.getTenantId();
        }

        // Fallback: extract from method arguments
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Parameter[] parameters = signature.getMethod().getParameters();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameters.length; i++) {
            if ("tenantId".equals(parameters[i].getName()) && args[i] instanceof UUID uuid) {
                return uuid;
            }
        }

        return null;
    }

    private UUID extractUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof TenantAuthenticationToken tenantAuth) {
            return tenantAuth.getUserId();
        }
        return null;
    }

    private String extractIpAddress() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String forwarded = request.getHeader("X-Forwarded-For");
                if (forwarded != null && !forwarded.isEmpty()) {
                    return forwarded.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.debug("Could not extract IP address: {}", e.getMessage());
        }
        return null;
    }

    private String serializeArgs(ProceedingJoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args.length > 0) {
                return objectMapper.writeValueAsString(args);
            }
        } catch (JsonProcessingException e) {
            log.debug("Could not serialize method arguments: {}", e.getMessage());
        }
        return null;
    }

    private String serializeResult(Object result) {
        try {
            if (result != null) {
                return objectMapper.writeValueAsString(result);
            }
        } catch (JsonProcessingException e) {
            log.debug("Could not serialize method result: {}", e.getMessage());
        }
        return null;
    }

    private UUID extractEntityId(Object result) {
        if (result instanceof com.mikelcrm.licenseservice.service.command.dto.CommandResponse commandResponse) {
            return commandResponse.getId();
        }
        return null;
    }
}
