package com.mikelcrm.licenseservice.service.command.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReactivateTenantRequest {

    @NotBlank
    private String metodoPago;

    @NotNull
    private BigDecimal montoEsperado;
}
