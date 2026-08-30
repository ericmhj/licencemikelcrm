package com.mikelcrm.licenseservice.controller;

import com.mikelcrm.licenseservice.payment.StripeProperties;
import com.mikelcrm.licenseservice.service.command.SpeiPaymentService;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Webhook endpoint for Stripe SPEI payment notifications.
 * Receives payment_intent.succeeded events when a tenant deposits via SPEI.
 *
 * Security: Validates Stripe webhook signature (HMAC SHA256).
 * This endpoint is public (no JWT required) — protected by signature verification.
 */
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
public class SpeiWebhookController {

    private final SpeiPaymentService speiPaymentService;
    private final StripeProperties stripeProperties;

    /**
     * POST /api/v1/webhooks/spei
     * Receives Stripe webhook events for SPEI bank transfer payments.
     */
    @PostMapping("/spei")
    public ResponseEntity<?> handleSpeiWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {

        // 1. Validar firma de Stripe
        if (sigHeader == null || sigHeader.isBlank()) {
            log.warn("[SpeiWebhook] Request sin Stripe-Signature header");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Missing signature"));
        }

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, stripeProperties.getWebhookSecret());
        } catch (Exception e) {
            log.error("[SpeiWebhook] Firma inválida: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid signature"));
        }

        // 2. Procesar solo eventos relevantes
        String eventType = event.getType();
        log.info("[SpeiWebhook] Evento recibido: type={}, id={}", eventType, event.getId());

        if ("payment_intent.succeeded".equals(eventType)) {
            return handlePaymentIntentSucceeded(event, payload);
        }

        // Eventos no manejados — responder 200 para que Stripe no reintente
        return ResponseEntity.ok(Map.of("received", true, "type", eventType));
    }

    private ResponseEntity<?> handlePaymentIntentSucceeded(Event event, String rawPayload) {
        try {
            StripeObject stripeObject = event.getDataObjectDeserializer()
                    .getObject()
                    .orElse(null);

            if (!(stripeObject instanceof PaymentIntent paymentIntent)) {
                log.error("[SpeiWebhook] No se pudo deserializar PaymentIntent");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid payload"));
            }

            // Extraer datos del pago SPEI
            String paymentIntentId = paymentIntent.getId();
            BigDecimal monto = BigDecimal.valueOf(paymentIntent.getAmount())
                    .movePointLeft(2); // centavos → pesos

            // Extraer CLABE destino y clave de rastreo del metadata o charges
            // Stripe incluye estos datos en payment_method_details.mx_bank_transfer
            String clabeDestino = extractClabe(paymentIntent);
            String claveRastreo = extractClaveRastreo(paymentIntent);
            String ordenanteNombre = extractOrdenanteNombre(paymentIntent);
            String ordenanteClabe = extractOrdenanteClabe(paymentIntent);

            if (clabeDestino == null || claveRastreo == null) {
                log.error("[SpeiWebhook] Datos SPEI incompletos en PaymentIntent {}", paymentIntentId);
                return ResponseEntity.ok(Map.of("received", true, "warning", "incomplete_spei_data"));
            }

            // 3. Procesar el pago
            speiPaymentService.procesarPagoSpei(
                    clabeDestino,
                    monto,
                    claveRastreo,
                    ordenanteNombre,
                    ordenanteClabe,
                    paymentIntent.getDescription(),
                    paymentIntentId,
                    rawPayload
            );

            return ResponseEntity.ok(Map.of("received", true, "applied", true));

        } catch (Exception e) {
            log.error("[SpeiWebhook] Error procesando payment_intent.succeeded: {}", e.getMessage(), e);
            // Retornar 200 para evitar reintentos infinitos — el error se loggea
            return ResponseEntity.ok(Map.of("received", true, "error", e.getMessage()));
        }
    }

    // ─── Helpers para extraer datos SPEI del PaymentIntent ───────────────

    /**
     * Extrae la CLABE destino del PaymentIntent.
     *
     * La CLABE destino identifica al tenant. Stripe la entrega en el metadata
     * del PaymentIntent (configurado al crear la referencia de pago SPEI por
     * tenant). No se usa getCharges() porque la librería Stripe Java moderna
     * expone únicamente el ID de la charge (getLatestCharge()), y para expandir
     * los detalles bancarios se requeriría una llamada adicional a la API.
     * El metadata es la fuente confiable y estable para este dato.
     */
    private String extractClabe(PaymentIntent pi) {
        if (pi.getMetadata() != null) {
            return pi.getMetadata().get("clabe_destino");
        }
        return null;
    }

    private String extractClaveRastreo(PaymentIntent pi) {
        try {
            if (pi.getMetadata() != null && pi.getMetadata().containsKey("clave_rastreo")) {
                return pi.getMetadata().get("clave_rastreo");
            }
            // Use PaymentIntent ID as fallback for tracking
            return pi.getId();
        } catch (Exception e) {
            return pi.getId();
        }
    }

    private String extractOrdenanteNombre(PaymentIntent pi) {
        try {
            if (pi.getMetadata() != null) {
                return pi.getMetadata().get("ordenante_nombre");
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String extractOrdenanteClabe(PaymentIntent pi) {
        try {
            if (pi.getMetadata() != null) {
                return pi.getMetadata().get("ordenante_clabe");
            }
        } catch (Exception ignored) {}
        return null;
    }
}
