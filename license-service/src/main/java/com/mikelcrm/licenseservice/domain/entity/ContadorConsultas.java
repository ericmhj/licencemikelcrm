package com.mikelcrm.licenseservice.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "contador_consultas", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tenant_id", "tipo_reporte", "periodo_anio"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContadorConsultas {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "tipo_reporte", nullable = false)
    private String tipoReporte;

    @Column(name = "total_consultas", nullable = false)
    private Integer totalConsultas;

    @Column(name = "periodo_anio", nullable = false)
    private Integer periodoAnio;

    @Column(name = "ultimo_incremento", nullable = false)
    private LocalDateTime ultimoIncremento;
}
