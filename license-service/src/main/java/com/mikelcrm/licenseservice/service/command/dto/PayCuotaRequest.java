package com.mikelcrm.licenseservice.service.command.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayCuotaRequest {

    @NotNull
    private LocalDate fechaPago;

    @NotBlank
    private String metodoPago;

    private String referenciaExternal;
}
