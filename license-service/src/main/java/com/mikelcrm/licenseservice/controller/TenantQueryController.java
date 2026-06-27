package com.mikelcrm.licenseservice.controller;

import com.mikelcrm.licenseservice.security.TenantAuthenticationToken;
import com.mikelcrm.licenseservice.service.query.ContractQueryService;
import com.mikelcrm.licenseservice.service.query.dto.ContractResponse;
import com.mikelcrm.licenseservice.service.query.dto.CreditResponse;
import com.mikelcrm.licenseservice.service.query.dto.ReactivationSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}")
@RequiredArgsConstructor
public class TenantQueryController {

    private final ContractQueryService contractQueryService;

    @GetMapping("/contracts")
    public ResponseEntity<ContractResponse> getContracts(
            @PathVariable UUID tenantId, Authentication auth) {
        if (!validateTenantConfinement(tenantId, auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        ContractResponse response = contractQueryService.getContracts(tenantId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/credits")
    public ResponseEntity<CreditResponse> getCredits(
            @PathVariable UUID tenantId, Authentication auth) {
        if (!validateTenantConfinement(tenantId, auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        CreditResponse response = contractQueryService.getCredits(tenantId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reactivation-summary")
    public ResponseEntity<ReactivationSummaryResponse> getReactivationSummary(
            @PathVariable UUID tenantId, Authentication auth) {
        if (!validateTenantConfinement(tenantId, auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        ReactivationSummaryResponse response = contractQueryService.getReactivationSummary(tenantId);
        return ResponseEntity.ok(response);
    }

    /**
     * Validates that the requesting user's tenantId matches the path tenantId (tenant confinement).
     */
    private boolean validateTenantConfinement(UUID pathTenantId, Authentication auth) {
        if (auth instanceof TenantAuthenticationToken tenantAuth) {
            return pathTenantId.equals(tenantAuth.getTenantId());
        }
        return false;
    }
}
