package com.mikelcrm.licenseservice.service.command.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterConsultationRequest {

    @NotBlank
    private String tipoReporte;

    @NotNull
    private UUID documentoId;

    @NotNull
    private UUID usuarioId;
}
