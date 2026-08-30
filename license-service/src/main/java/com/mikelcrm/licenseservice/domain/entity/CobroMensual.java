package com.mikelcrm.licenseservice.domain.entity;

import com.mikelcrm.licenseservice.domain.enums.EstadoCobroMensual;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Registro de cobro mensual por plan contratado.
 * Se genera automáticamente el día 1 de cada mes para tenants activos.
 * Se marca como PAGADO cuando se recibe el SPEI correspondiente.
 */
@Entity
@Table(name = "cobro_mensual",
       uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "periodo_mes"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CobroMensual {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(name = "plan_nombre", length = 100, nullable = false)
    private String planNombre;

    @Column(name = "periodo_mes", nullable = false)
    private LocalDate periodoMes;

    @Column(name = "monto_plan", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoPlan;

    @Column(name = "creditos_otorgados", nullable = false)
    private Integer creditosOtorgados;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoCobroMensual estado = EstadoCobroMensual.PENDIENTE;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    @Column(name = "clave_rastreo", length = 50)
    private String claveRastreo;

    @Column(name = "referencia_pago", length = 100)
    private String referenciaPago;

    @Column(name = "stripe_invoice_id", length = 100)
    private String stripeInvoiceId;

    @Column(name = "generado_en", nullable = false)
    @Builder.Default
    private LocalDateTime generadoEn = LocalDateTime.now();
}
