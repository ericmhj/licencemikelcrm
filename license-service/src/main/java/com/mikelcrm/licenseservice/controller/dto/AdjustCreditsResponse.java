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
public class AdjustCreditsResponse {
    private String status;          // "approved" or "rejected"
    private BigDecimal saldoResultante;
    private String eventoId;
    private String timestamp;
    private String message;
}
