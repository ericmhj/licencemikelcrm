package com.mikelcrm.licenseservice.controller;

import com.mikelcrm.licenseservice.service.command.CuotaPaymentService;
import com.mikelcrm.licenseservice.service.command.dto.CommandResponse;
import com.mikelcrm.licenseservice.service.command.dto.PayCuotaRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/contracts/{contratoId}/cuotas")
@RequiredArgsConstructor
public class CuotaCommandController {

    private final CuotaPaymentService cuotaPaymentService;

    @PostMapping("/{cuotaId}/pay")
    public ResponseEntity<CommandResponse> payCuota(
            @PathVariable UUID tenantId,
            @PathVariable UUID contratoId,
            @PathVariable UUID cuotaId,
            @Valid @RequestBody PayCuotaRequest request) {
        CommandResponse response = cuotaPaymentService.payCuota(tenantId, contratoId, cuotaId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
