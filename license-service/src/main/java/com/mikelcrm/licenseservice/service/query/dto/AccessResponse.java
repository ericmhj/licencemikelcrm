package com.mikelcrm.licenseservice.service.query.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessResponse {
    private UUID tenantId;
    private String status;
    private List<String> modules;
    private BigDecimal creditBalance;
    private String userRole;
    private String cachedAt;
}
