package com.mikelcrm.licenseservice.controller;

import com.mikelcrm.licenseservice.service.command.TenantCommandService;
import com.mikelcrm.licenseservice.service.command.dto.CommandResponse;
import com.mikelcrm.licenseservice.service.command.dto.CreateTenantRequest;
import com.mikelcrm.licenseservice.service.command.dto.ReactivateTenantRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
public class TenantCommandController {

    private final TenantCommandService tenantCommandService;

    @PostMapping
    public ResponseEntity<CommandResponse> createTenant(@Valid @RequestBody CreateTenantRequest request) {
        CommandResponse response = tenantCommandService.createTenant(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @PostMapping("/{tenantId}/activate")
    public ResponseEntity<CommandResponse> activateTenant(@PathVariable UUID tenantId) {
        CommandResponse response = tenantCommandService.activateTenant(tenantId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @PostMapping("/{tenantId}/suspend")
    public ResponseEntity<CommandResponse> suspendTenant(@PathVariable UUID tenantId) {
        CommandResponse response = tenantCommandService.suspendTenant(tenantId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @PostMapping("/{tenantId}/reactivate")
    public ResponseEntity<CommandResponse> reactivateTenant(
            @PathVariable UUID tenantId,
            @Valid @RequestBody ReactivateTenantRequest request) {
        CommandResponse response = tenantCommandService.reactivateTenant(tenantId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @PostMapping("/{tenantId}/cancel")
    public ResponseEntity<CommandResponse> cancelTenant(@PathVariable UUID tenantId) {
        CommandResponse response = tenantCommandService.cancelTenant(tenantId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
