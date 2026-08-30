package com.mikelcrm.licenseservice.domain.entity;

import com.mikelcrm.licenseservice.domain.enums.TipoCobro;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * CLABE SPEI asignada a un tenant para recibir pagos.
 * Cada tenant puede tener una CLABE para cobro fijo mensual
 * y/o una para abonos variables prepago.
 * Las CLABEs son generadas por Stripe (mx_bank_transfer).
 */
@Entity
@Table(name = "cuenta_clabe_tenant",
       uniqueConstraints = @UniqueConstraint(columnNames = {"clabe"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CuentaClabeTenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(length = 18, nullable = false, unique = true)
    private String clabe;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoCobro tipo;

    @Column(name = "stripe_customer_id", length = 100)
    private String stripeCustomerId;

    @Column(name = "psp", length = 50, nullable = false)
    @Builder.Default
    private String psp = "STRIPE";

    @Column(nullable = false)
    @Builder.Default
    private Boolean activa = true;

    @Column(name = "creada_en", nullable = false)
    @Builder.Default
    private LocalDateTime creadaEn = LocalDateTime.now();
}
