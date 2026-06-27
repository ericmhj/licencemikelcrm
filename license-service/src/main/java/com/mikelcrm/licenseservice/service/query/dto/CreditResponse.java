package com.mikelcrm.licenseservice.service.query.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditResponse {

    private BigDecimal saldoDisponible;
    private int creditosTotalesAdquiridos;
    private int umbralConsultasIncluidas;
    private int contadorGlobalConsultas;
    private int excedente;
    private PaqueteActivo paqueteActivo;
    private String alertaNivel;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaqueteActivo {
        private UUID paqueteId;
        private int creditosPaquete;
        private int creditosBonus;
        private LocalDate fechaVencimiento;
    }
}
