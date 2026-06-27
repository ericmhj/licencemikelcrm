package com.mikelcrm.licenseservice.service.query.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReactivationSummaryResponse {

    private UUID tenantId;
    private long diasEnMora;
    private List<CuotaVencidaDetail> cuotasVencidas;
    private List<CuotaAlmacenamientoDetail> cuotasAlmacenamiento;
    private BigDecimal totalCuotasVencidas;
    private BigDecimal totalAlmacenamiento;
    private BigDecimal totalParaReactivar;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CuotaVencidaDetail {
        private UUID cuotaId;
        private String periodo;
        private BigDecimal monto;
        private LocalDate fechaLimite;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CuotaAlmacenamientoDetail {
        private String periodo;
        private int reportes;
        private BigDecimal monto;
    }
}
