package com.mikelcrm.licenseservice.domain.entity;

import com.mikelcrm.licenseservice.domain.enums.RolTenant;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "rol_usuario")
@IdClass(RolUsuarioId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolUsuario {

    @Id
    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RolTenant rol;

    @Column(name = "asignado_en", nullable = false)
    private LocalDateTime asignadoEn;

    @Column(name = "asignado_por")
    private UUID asignadoPor;
}
