package com.mikelcrm.licenseservice.domain.entity;

import com.mikelcrm.licenseservice.domain.enums.EstadoPaquete;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "paquete_creditos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaqueteCreditos {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contrato_id")
    private ContratoAnual contrato;

    @Column(name = "creditos_paquete", nullable = false)
    private Integer creditosPaquete;

    @Column(name = "creditos_bonus", nullable = false)
    private Integer creditosBonus;

    @Column(name = "saldo_disponible", nullable = false, precision = 10, scale = 2)
    private BigDecimal saldoDisponible;

    @Column(name = "creditos_totales_adquiridos", nullable = false)
    private Integer creditosTotalesAdquiridos;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPaquete estado;
}
