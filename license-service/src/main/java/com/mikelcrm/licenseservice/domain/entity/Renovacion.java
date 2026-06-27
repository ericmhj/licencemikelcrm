package com.mikelcrm.licenseservice.domain.entity;

import com.mikelcrm.licenseservice.domain.enums.EstadoRenovacion;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "renovacion")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Renovacion {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contrato_id", nullable = false)
    private ContratoAnual contrato;

    @Column(name = "fecha_vencimiento_contrato", nullable = false)
    private LocalDate fechaVencimientoContrato;

    @Column(name = "tarifa_renovacion", nullable = false, precision = 10, scale = 2)
    private BigDecimal tarifaRenovacion;

    @Column(name = "primera_cuota", nullable = false, precision = 10, scale = 2)
    private BigDecimal primeraCuota;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoRenovacion estado;

    @Column(name = "notificado_30d")
    private LocalDateTime notificado30d;

    @Column(name = "notificado_7d")
    private LocalDateTime notificado7d;

    @Column(name = "notificado_1d")
    private LocalDateTime notificado1d;

    @Column(name = "procesada_en")
    private LocalDateTime procesadaEn;
}
