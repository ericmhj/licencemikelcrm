package com.mikelcrm.licenseservice.domain.entity;

import com.mikelcrm.licenseservice.domain.enums.TipoMovimientoEdoCuenta;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Estado de cuenta por tenant — registro append-only de todos los movimientos financieros.
 * Solo abonos (pagos recibidos) y cargos (cobros mensuales).
 * Permite al tenant y al admin ver su historial completo de pagos.
 */
@Entity
@Table(name = "estado_cuenta_tenant")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstadoCuentaTenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMovimientoEdoCuenta tipo;  // ABONO | CARGO

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "saldo_resultante", nullable = false, precision = 12, scale = 2)
    private BigDecimal saldoResultante;

    @Column(nullable = false, length = 255)
    private String concepto;

    @Column(name = "referencia", length = 100)
    private String referencia;

    @Column(name = "clave_rastreo", length = 50)
    private String claveRastreo;

    @Column(name = "periodo_mes")
    private String periodoMes;

    @Column(name = "cobro_mensual_id")
    private UUID cobroMensualId;

    @Column(name = "pago_spei_id")
    private UUID pagoSpeiId;

    @Column(name = "registrado_en", nullable = false)
    @Builder.Default
    private LocalDateTime registradoEn = LocalDateTime.now();
}
