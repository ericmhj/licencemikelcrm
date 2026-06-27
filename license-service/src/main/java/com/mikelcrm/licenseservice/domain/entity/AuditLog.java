package com.mikelcrm.licenseservice.domain.entity;

import com.mikelcrm.licenseservice.domain.enums.ResultadoAudit;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_log")
@IdClass(AuditLogId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    private UUID id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(nullable = false)
    private String accion;

    @Column(nullable = false)
    private String entidad;

    @Column(name = "entidad_id")
    private UUID entidadId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_antes", columnDefinition = "jsonb")
    private String payloadAntes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_despues", columnDefinition = "jsonb")
    private String payloadDespues;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResultadoAudit resultado;

    @Column(name = "ip_origen")
    private String ipOrigen;

    @Id
    @Column(name = "ocurrido_en", nullable = false)
    private LocalDateTime ocurridoEn;
}
