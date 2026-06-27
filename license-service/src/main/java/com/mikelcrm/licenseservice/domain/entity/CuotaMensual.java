package com.mikelcrm.licenseservice.domain.entity;

import com.mikelcrm.licenseservice.domain.enums.EstadoCuota;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cuota_mensual", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"contrato_id", "numero"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CuotaMensual {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contrato_id", nullable = false)
    private ContratoAnual contrato;

    @Column(nullable = false)
    private Integer numero;

    @Column(name = "fecha_limite", nullable = false)
    private LocalDate fechaLimite;

    @Column(name = "monto_original", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoOriginal;

    @Column(name = "monto_cobrado", precision = 10, scale = 2)
    private BigDecimal montoCobrado;

    @Column(name = "descuento_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal descuentoPct;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCuota estado;

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    @Column(name = "intento_cobro", nullable = false)
    private Integer intentoCobro;
}
