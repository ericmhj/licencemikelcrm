package com.mikelcrm.licenseservice.audit;

import com.mikelcrm.licenseservice.domain.entity.AuditLog;
import com.mikelcrm.licenseservice.domain.enums.ResultadoAudit;
import com.mikelcrm.licenseservice.domain.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for recording audit log entries.
 * Provides a simple API for logging both successful and denied actions.
 * All writes are asynchronous to avoid blocking the response path.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Registers a denied access attempt in the audit log.
     * Called by the RBACInterceptor and TenantConfinementAspect when access is denied.
     *
     * @param tenantId  the tenant attempting access
     * @param userId    the user attempting access
     * @param accion    the action being attempted
     * @param entidad   the resource/entity being accessed
     * @param motivo    the reason for denial
     * @param ipOrigen  the source IP address
     */
    @Async
    public void registrarAccesoDenegado(UUID tenantId, UUID userId, String accion,
                                         String entidad, String motivo, String ipOrigen) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .id(UUID.randomUUID())
                    .tenantId(tenantId)
                    .usuarioId(userId)
                    .accion(accion)
                    .entidad(entidad)
                    .entidadId(null)
                    .payloadAntes(null)
                    .payloadDespues("{\"motivo\":\"" + motivo + "\"}")
                    .resultado(ResultadoAudit.DENEGADO)
                    .ipOrigen(ipOrigen)
                    .ocurridoEn(LocalDateTime.now())
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log (DENEGADO): accion={}, entidad={}, tenantId={}, motivo={}",
                    accion, entidad, tenantId, motivo);
        } catch (Exception e) {
            log.error("Failed to save denied access audit log: {}", e.getMessage());
        }
    }

    /**
     * Registers a successful action in the audit log.
     */
    @Async
    public void registrarAccionExitosa(UUID tenantId, UUID userId, String accion,
                                        String entidad, UUID entidadId,
                                        String payloadAntes, String payloadDespues,
                                        String ipOrigen) {
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
                    .resultado(ResultadoAudit.OK)
                    .ipOrigen(ipOrigen)
                    .ocurridoEn(LocalDateTime.now())
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log (OK): accion={}, entidad={}, tenantId={}", accion, entidad, tenantId);
        } catch (Exception e) {
            log.error("Failed to save audit log: {}", e.getMessage());
        }
    }
}
