package com.mikelcrm.licenseservice.controller;

import com.mikelcrm.licenseservice.service.command.AperturaContractService;
import com.mikelcrm.licenseservice.service.command.dto.AperturaRequest;
import com.mikelcrm.licenseservice.service.command.dto.CommandResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for contract opening (apertura) and upgrade flows.
 */
@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/apertura")
@RequiredArgsConstructor
public class AperturaController {

    private final AperturaContractService aperturaContractService;

    /**
     * Process apertura (contract opening) for a tenant.
     * Supports ESTANDAR (400€, 2 credits) and PERSONALIZADO (1,400€, 10 credits).
     */
    @PostMapping
    public ResponseEntity<CommandResponse> processApertura(
            @PathVariable UUID tenantId,
            @Valid @RequestBody AperturaRequest request) {
        CommandResponse response = aperturaContractService.processApertura(tenantId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Upgrade from Estándar to Personalizado.
     * Costs 1,000€ (difference) and grants 8 additional credits.
     */
    @PostMapping("/upgrade")
    public ResponseEntity<CommandResponse> upgradeToPersonalizado(@PathVariable UUID tenantId) {
        CommandResponse response = aperturaContractService.upgradeToPersonalizado(tenantId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
