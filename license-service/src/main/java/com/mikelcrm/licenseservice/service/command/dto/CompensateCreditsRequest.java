package com.mikelcrm.licenseservice.service.command.dto;

import com.mikelcrm.licenseservice.domain.enums.PerfilDocumento;
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
public class CompensateCreditsRequest {

    @NotNull
    private UUID documentoId;

    @NotNull
    private PerfilDocumento perfilDocumento;
}
