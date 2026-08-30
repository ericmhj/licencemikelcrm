package com.mikelcrm.licenseservice.domain.entity;

import com.mikelcrm.licenseservice.domain.enums.EstadoPagoSpei;
import com.mikelcrm.licenseservice.domain.enums.TipoCobro;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Registro de cada pago SPEI recibido vía webhook de Stripe.
 * La clave_rastreo es única (idempotencia natural de SPEI).
 */
@Entity
@Table(name = "pago_spei_recibido",
       uniqueConstraints = @UniqueConstraint(columnNames = {"clave_rastreo"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagoSpeiRecibido {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "clabe_destino", length = 18, nullable = false)
    private String clabeDestino;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "clave_rastreo", length = 50, nullable = false, unique = true)
    private String claveRastreo;

    @Column(name = "ordenante_nombre", length = 200)
    private String ordenanteNombre;

    @Column(name = "ordenante_clabe", length = 18)
    private String ordenanteClabe;

    @Column(name = "concepto_pago", length = 255)
    private String conceptoPago;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_cobro", nullable = false)
    private TipoCobro tipoCobro;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoPagoSpei estado = EstadoPagoSpei.RECIBIDO;

    @Column(name = "cobro_mensual_id")
    private UUID cobroMensualId;

    @Column(name = "creditos_otorgados", precision = 10, scale = 2)
    private BigDecimal creditosOtorgados;

    @Column(name = "stripe_payment_intent_id", length = 100)
    private String stripePaymentIntentId;

    @Column(name = "recibido_en", nullable = false)
    @Builder.Default
    private LocalDateTime recibidoEn = LocalDateTime.now();

    @Column(name = "aplicado_en")
    private LocalDateTime aplicadoEn;

    @Column(name = "payload_raw", columnDefinition = "TEXT")
    private String payloadRaw;
}
