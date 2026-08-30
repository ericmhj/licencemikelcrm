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
public class LedgerEntryResponse {
    private String id;
    private String tipo;
    private BigDecimal cantidad;
    private BigDecimal saldoResultante;
    private String concepto;
    private String perfilDocumento;
    private String referencia;
    private String actorId;
    private String actorEmail;
    private String createdAt;
}
