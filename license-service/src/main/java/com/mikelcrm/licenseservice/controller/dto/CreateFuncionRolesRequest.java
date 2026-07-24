package com.mikelcrm.licenseservice.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateFuncionRolesRequest(
        @NotBlank String codigo,
        String descripcion,
        @NotBlank String funcionalidad,
        @NotEmpty List<String> roles
) {}
