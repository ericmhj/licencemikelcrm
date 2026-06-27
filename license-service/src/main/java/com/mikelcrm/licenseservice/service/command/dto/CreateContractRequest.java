package com.mikelcrm.licenseservice.service.command.dto;

import com.mikelcrm.licenseservice.domain.enums.TipoContrato;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateContractRequest {

    @NotNull
    private TipoContrato tipo;

    private String modulo; // Required for MODULO type

    @NotNull
    private BigDecimal cuotaMensual;

    @NotNull
    private LocalDate fechaInicio;

    private boolean renovacionAuto = true;
}
