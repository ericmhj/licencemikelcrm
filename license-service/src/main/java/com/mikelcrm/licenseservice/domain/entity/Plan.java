package com.mikelcrm.licenseservice.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "planes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 50)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "creditos_mensuales", nullable = false)
    private Integer creditosMensuales;

    @Column(name = "max_free_downloads", nullable = false)
    private Integer maxFreeDownloads;

    @Column(name = "max_usuarios", nullable = false)
    private Integer maxUsuarios;

    @com.fasterxml.jackson.annotation.JsonRawValue
    @Column(name = "roles_autorizados", nullable = false, columnDefinition = "TEXT")
    private String rolesAutorizados;

    @Column(name = "precio_mensual", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioMensual;

    /**
     * Créditos que se descuentan de la cartera por cada reporte de estudio generado.
     * Varía por plan. Valor base: 520.
     */
    @Column(name = "costo_reporte", nullable = false)
    @Builder.Default
    private Integer costoReporte = 520;

    /**
     * Créditos que se descuentan de la cartera por cada punto de muestreo del estudio.
     * Se multiplica por el número de puntos. Varía por plan. Valor base: 35.
     */
    @Column(name = "costo_punto_muestreo", nullable = false)
    @Builder.Default
    private Integer costoPuntoMuestreo = 35;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
