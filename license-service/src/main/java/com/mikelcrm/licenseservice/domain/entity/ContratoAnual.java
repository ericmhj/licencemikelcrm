package com.mikelcrm.licenseservice.domain.entity;

import com.mikelcrm.licenseservice.domain.enums.EstadoContrato;
import com.mikelcrm.licenseservice.domain.enums.TipoContrato;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "contrato_anual")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContratoAnual {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoContrato tipo;

    @Column
    private String modulo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoContrato estado;

    @Column(name = "cuota_mensual", nullable = false, precision = 10, scale = 2)
    private BigDecimal cuotaMensual;

    @Column(name = "cuotas_pagadas", nullable = false)
    private Integer cuotasPagadas;

    @Column(name = "cuotas_totales", nullable = false)
    private Integer cuotasTotales;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Column(name = "fecha_aniversario", nullable = false)
    private Integer fechaAniversario;

    @Column(name = "renovacion_auto", nullable = false)
    private Boolean renovacionAuto;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;
}
