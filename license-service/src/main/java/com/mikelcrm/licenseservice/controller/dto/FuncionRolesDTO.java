package com.mikelcrm.licenseservice.controller.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record FuncionRolesDTO(
        UUID id,
        String codigo,
        String descripcion,
        String funcionalidad,
        List<String> roles,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
