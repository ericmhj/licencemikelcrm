package com.mikelcrm.licenseservice.service.command.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Solicitud de consumo de créditos por la generación de un reporte de estudio.
 * El costo total = costoReporte(plan) + costoPuntoMuestreo(plan) * numeroPuntos.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumeReportRequest {

    /** Identificador del reporte/estudio generado. */
    @NotNull
    private UUID documentoId;

    /** Usuario que generó el reporte. */
    @NotNull
    private UUID usuarioId;

    /** Número de puntos de muestreo del estudio (>= 0). */
    @NotNull
    @Min(value = 0, message = "El número de puntos de muestreo no puede ser negativo")
    private Integer numeroPuntos;
}
