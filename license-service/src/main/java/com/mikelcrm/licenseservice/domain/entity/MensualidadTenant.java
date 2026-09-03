package com.mikelcrm.licenseservice.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Renta mensual del CRM por tenant. Se genera una fila por cada mes desde la
 * fecha de alta del tenant. El pago se aplica al mes PENDIENTE más antiguo y
 * avanza hacia el más reciente para habilitar el servicio.
 */
@Entity
@Table(name = "mensualidad_tenant",
       uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "periodo_mes"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MensualidadTenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    /** Primer día del mes cubierto por esta mensualidad. */
    @Column(name = "periodo_mes", nullable = false)
    private LocalDate periodoMes;

    /** Mensualidad esperada (snapshot del precio del plan). */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    /** PENDIENTE | PAGADA */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String estado = "PENDIENTE";

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    @Column(name = "pago_spei_id")
    private UUID pagoSpeiId;

    @Column(length = 255)
    private String concepto;

    @Column(name = "creada_en", nullable = false)
    @Builder.Default
    private LocalDateTime creadaEn = LocalDateTime.now();
}
