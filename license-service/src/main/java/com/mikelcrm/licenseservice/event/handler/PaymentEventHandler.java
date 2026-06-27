package com.mikelcrm.licenseservice.event.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mikelcrm.licenseservice.service.command.CuotaPaymentService;
import com.mikelcrm.licenseservice.service.command.TenantCommandService;
import com.mikelcrm.licenseservice.service.command.dto.PayCuotaRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventHandler {

    private final TenantCommandService tenantCommandService;
    private final CuotaPaymentService cuotaPaymentService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "payment-events", groupId = "license-service-group")
    public void handlePaymentEvent(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String eventType = root.path("eventType").asText();
            JsonNode payload = root.path("payload");

            switch (eventType) {
                case "payment.apertura.confirmed" -> handleAperturaConfirmed(payload);
                case "payment.cuota.confirmed" -> handleCuotaConfirmed(payload);
                case "payment.failed" -> handlePaymentFailed(payload);
                default -> log.warn("Unknown payment event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing payment event: {}", e.getMessage(), e);
        }
    }

    private void handleAperturaConfirmed(JsonNode payload) {
        UUID tenantId = UUID.fromString(payload.path("tenantId").asText());
        log.info("Processing apertura confirmation for tenant {}", tenantId);

        try {
            tenantCommandService.activateTenant(tenantId);
            log.info("Tenant {} activated after apertura payment confirmation", tenantId);
        } catch (Exception e) {
            log.error("Failed to activate tenant {} after apertura confirmation: {}", tenantId, e.getMessage(), e);
        }
    }

    private void handleCuotaConfirmed(JsonNode payload) {
        UUID tenantId = UUID.fromString(payload.path("tenantId").asText());
        UUID contratoId = UUID.fromString(payload.path("contratoId").asText());
        UUID cuotaId = UUID.fromString(payload.path("cuotaId").asText());
        String fechaPagoStr = payload.path("fechaPago").asText();

        log.info("Processing cuota payment confirmation for tenant {}, cuota {}", tenantId, cuotaId);

        try {
            PayCuotaRequest request = new PayCuotaRequest();
            request.setFechaPago(LocalDate.parse(fechaPagoStr));

            cuotaPaymentService.payCuota(tenantId, contratoId, cuotaId, request);
            log.info("Cuota {} paid successfully for tenant {}", cuotaId, tenantId);
        } catch (Exception e) {
            log.error("Failed to process cuota payment for tenant {}, cuota {}: {}", tenantId, cuotaId, e.getMessage(), e);
        }
    }

    private void handlePaymentFailed(JsonNode payload) {
        UUID tenantId = UUID.fromString(payload.path("tenantId").asText());
        String reason = payload.path("reason").asText("unknown");
        String paymentReference = payload.path("paymentReference").asText("N/A");

        log.warn("Payment failed for tenant {}. Reason: {}, Reference: {}", tenantId, reason, paymentReference);
    }
}
