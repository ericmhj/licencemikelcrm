package com.mikelcrm.licenseservice.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantBalanceResponse {
    private String tenantId;
    private String slug;
    private String nombre;
    private String plan;
    private String estado;
    private BigDecimal saldoCreditos;
    private int creditosTotalesAdquiridos;
    private int creditosConsumidos;
    private String ultimoSync;
}
