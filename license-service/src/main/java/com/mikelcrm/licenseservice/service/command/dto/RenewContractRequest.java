package com.mikelcrm.licenseservice.service.command.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RenewContractRequest {

    @NotBlank
    private String metodoPago;
}
