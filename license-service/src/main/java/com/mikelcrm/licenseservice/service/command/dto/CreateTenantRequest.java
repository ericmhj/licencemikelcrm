package com.mikelcrm.licenseservice.service.command.dto;

import com.mikelcrm.licenseservice.domain.enums.ModalidadReporte;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTenantRequest {

    @NotBlank
    private String nombre;

    @NotBlank
    @Email
    private String emailContacto;

    @NotNull
    private ModalidadReporte modalidadApertura;
}
