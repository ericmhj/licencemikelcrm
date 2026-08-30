package com.mikelcrm.licenseservice.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Request payload for adjusting a tenant's credit balance.
 * Only accessible by platform_admin role.
 */
@Data
public class AdjustCreditsRequest {

    /**
     * Client-generated UUID for idempotency.
     * If a request with this ID was already processed, it will be rejected.
     */
    @NotBlank(message = "operationId es obligatorio para idempotencia")
    private String operationId;

    /**
     * Type of adjustment: 'recarga' (add) or 'ajuste' (subtract).
     */
    @NotBlank(message = "tipo es obligatorio")
    private String tipo;

    /**
     * Amount to adjust. Positive for recarga, negative for ajuste.
     * Backend validates: absolute value between 0.5 and 500.
     */
    @NotNull(message = "cantidad es obligatoria")
    private BigDecimal cantidad;

    /**
     * Mandatory reason for the adjustment (minimum 20 characters).
     * Stored in audit log for accountability.
     */
    @NotBlank(message = "motivo es obligatorio")
    @Size(min = 3, max = 500, message = "El motivo debe tener entre 3 y 500 caracteres")
    private String motivo;

    /**
     * Número de referencia obligatorio (ej: folio de pago, transferencia, factura).
     */
    @NotBlank(message = "referencia es obligatoria")
    @Size(min = 3, max = 10, message = "La referencia debe tener entre 3 y 10 caracteres")
    private String referencia;

    /**
     * Número de autorización obligatorio (ej: autorización bancaria, aprobación interna).
     */
    @NotBlank(message = "número de autorización es obligatorio")
    @Size(min = 3, max = 20, message = "La autorización debe tener entre 3 y 20 caracteres")
    private String autorizacion;
}
