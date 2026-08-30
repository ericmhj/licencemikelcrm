package com.mikelcrm.licenseservice.controller;

import com.mikelcrm.licenseservice.service.command.ConsultationService;
import com.mikelcrm.licenseservice.service.command.CreditCommandService;
import com.mikelcrm.licenseservice.service.command.dto.AcquireCreditsRequest;
import com.mikelcrm.licenseservice.service.command.dto.CommandResponse;
import com.mikelcrm.licenseservice.service.command.dto.CompensateCreditsRequest;
import com.mikelcrm.licenseservice.service.command.dto.ConsumeCreditsRequest;
import com.mikelcrm.licenseservice.service.command.dto.ConsumeReportRequest;
import com.mikelcrm.licenseservice.service.command.dto.RegisterConsultationRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}")
@RequiredArgsConstructor
public class CreditCommandController {

    private final CreditCommandService creditCommandService;
    private final ConsultationService consultationService;

    /**
     * Task 7.1: Acquire a credit package.
     */
    @PostMapping("/credits/packages")
    public ResponseEntity<CommandResponse> acquirePackage(
            @PathVariable UUID tenantId,
            @Valid @RequestBody AcquireCreditsRequest request) {
        CommandResponse response = creditCommandService.acquirePackage(tenantId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Task 7.3: Consume credits for PDF generation.
     */
    @PostMapping("/credits/consume")
    public ResponseEntity<CommandResponse> consumeCredits(
            @PathVariable UUID tenantId,
            @Valid @RequestBody ConsumeCreditsRequest request) {
        CommandResponse response = creditCommandService.consumeCredits(tenantId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Consume créditos por la generación de un reporte de estudio.
     * Costo = costoReporte(plan) + costoPuntoMuestreo(plan) * numeroPuntos.
     */
    @PostMapping("/reportes/consumo")
    public ResponseEntity<CommandResponse> consumeReport(
            @PathVariable UUID tenantId,
            @Valid @RequestBody ConsumeReportRequest request) {
        CommandResponse response = creditCommandService.consumeReport(tenantId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Task 7.7: Compensate credits (round-trip after PDF generation failure).
     */
    @PostMapping("/credits/compensate")
    public ResponseEntity<CommandResponse> compensateCredits(
            @PathVariable UUID tenantId,
            @Valid @RequestBody CompensateCreditsRequest request) {
        CommandResponse response = creditCommandService.compensateCredits(tenantId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Task 7.9: Register a consultation and increment counters.
     */
    @PostMapping("/consultations")
    public ResponseEntity<Map<String, Object>> registerConsultation(
            @PathVariable UUID tenantId,
            @Valid @RequestBody RegisterConsultationRequest request) {
        int globalCount = consultationService.registerConsultation(tenantId, request);
        return ResponseEntity.ok(Map.of("globalCount", globalCount));
    }
}
