package com.mikelcrm.licenseservice.domain.entity;

import com.mikelcrm.licenseservice.domain.enums.EstadoTenant;
import com.mikelcrm.licenseservice.domain.enums.ModalidadReporte;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenant")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String nombre;

    @Column(name = "email_contacto", nullable = false)
    private String emailContacto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoTenant estado;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidad_reporte", nullable = false)
    private ModalidadReporte modalidadReporte;

    @Column(name = "fecha_alta", nullable = false)
    private LocalDateTime fechaAlta;

    /**
     * Primer día del mes ya cubierto por un pago mensual (SPEI).
     * null = el tenant nunca ha pagado. Cada pago lo fija al primer día del mes en curso.
     * Un tenant se considera al corriente si servicioPagadoHasta >= primer día del mes actual.
     */
    @Column(name = "servicio_pagado_hasta")
    private java.time.LocalDate servicioPagadoHasta;

    @Column(name = "fecha_suspension")
    private LocalDateTime fechaSuspension;

    @Column(name = "fecha_cancelacion")
    private LocalDateTime fechaCancelacion;

    @Column(name = "reportes_almacenados", nullable = false)
    private Integer reportesAlmacenados;

    @Column(name = "deuda_almacenamiento", nullable = false, precision = 10, scale = 2)
    private BigDecimal deudaAlmacenamiento;

    @Column(name = "plan_id")
    private UUID planId;
}
