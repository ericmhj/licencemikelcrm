package com.mikelcrm.licenseservice.service.query.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuspendedResponse {
    private UUID tenantId;
    private String status;
    private String suspendedSince;
    private BigDecimal adeudoTotal;
    private String reactivationUrl;
}
