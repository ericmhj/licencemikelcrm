package com.mikelcrm.licenseservice.service.command.dto;

import com.mikelcrm.licenseservice.domain.enums.ModalidadReporte;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for processing contract opening (apertura).
 * Supports two modalities:
 * - ESTANDAR: 400€, 2 welcome credits
 * - PERSONALIZADO: 1,400€, 10 welcome credits
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AperturaRequest {

    @NotNull(message = "La modalidad es obligatoria")
    private ModalidadReporte modalidad;

    @NotBlank(message = "El método de pago es obligatorio")
    private String metodoPago;
}
