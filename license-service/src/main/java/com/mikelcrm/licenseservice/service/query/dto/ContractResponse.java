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
public class ContractResponse {

    private List<ContractDetail> contracts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContractDetail {
        private UUID contratoId;
        private String tipo;
        private String modulo;
        private String estado;
        private BigDecimal cuotaMensual;
        private int cuotasPagadas;
        private int cuotasTotales;
        private LocalDate proximaFechaCobro;
        private LocalDate fechaVencimientoContrato;
        private boolean renovacionAuto;
        private int descuentoDisponibleHoy;
        private BigDecimal montoConDescuentoHoy;
    }
}
