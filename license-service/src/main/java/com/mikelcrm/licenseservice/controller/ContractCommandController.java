package com.mikelcrm.licenseservice.controller;

import com.mikelcrm.licenseservice.service.command.ContractCommandService;
import com.mikelcrm.licenseservice.service.command.dto.CommandResponse;
import com.mikelcrm.licenseservice.service.command.dto.CreateContractRequest;
import com.mikelcrm.licenseservice.service.command.dto.RenewContractRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/contracts")
@RequiredArgsConstructor
public class ContractCommandController {

    private final ContractCommandService contractCommandService;

    @PostMapping
    public ResponseEntity<CommandResponse> createContract(
            @PathVariable UUID tenantId,
            @Valid @RequestBody CreateContractRequest request) {
        CommandResponse response = contractCommandService.createContract(tenantId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @DeleteMapping("/{contratoId}")
    public ResponseEntity<CommandResponse> requestDowngrade(
            @PathVariable UUID tenantId,
            @PathVariable UUID contratoId) {
        CommandResponse response = contractCommandService.requestDowngrade(tenantId, contratoId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @PostMapping("/{contratoId}/renew")
    public ResponseEntity<CommandResponse> renewContract(
            @PathVariable UUID tenantId,
            @PathVariable UUID contratoId,
            @Valid @RequestBody RenewContractRequest request) {
        CommandResponse response = contractCommandService.renewContract(tenantId, contratoId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
