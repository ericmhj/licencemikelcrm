package com.mikelcrm.licenseservice.domain.entity;

import com.mikelcrm.licenseservice.domain.enums.EstadoCuotaAlmacenamiento;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cuota_almacenamiento", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tenant_id", "periodo_mes"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CuotaAlmacenamiento {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "periodo_mes", nullable = false)
    private LocalDate periodoMes;

    @Column(name = "reportes_snapshot", nullable = false)
    private Integer reportesSnapshot;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCuotaAlmacenamiento estado;

    @Column(name = "generada_en", nullable = false)
    private LocalDateTime generadaEn;
}
