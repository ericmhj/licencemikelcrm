package com.mikelcrm.licenseservice.domain.entity;

import com.mikelcrm.licenseservice.domain.enums.TipoEventoCredito;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "evento_credito")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventoCredito {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paquete_id", nullable = false)
    private PaqueteCreditos paquete;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoEventoCredito tipo;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal cantidad;

    @Column(name = "saldo_resultante", nullable = false, precision = 10, scale = 2)
    private BigDecimal saldoResultante;

    @Column(name = "perfil_documento")
    private String perfilDocumento;

    @Column(name = "costo_creditos_aplicado", precision = 10, scale = 2)
    private BigDecimal costoCreditosAplicado;

    @Column(name = "documento_id")
    private UUID documentoId;

    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(name = "ocurrido_en", nullable = false)
    private LocalDateTime ocurridoEn;
}
