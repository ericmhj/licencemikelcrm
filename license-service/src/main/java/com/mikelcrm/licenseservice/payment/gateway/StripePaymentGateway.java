package com.mikelcrm.licenseservice.payment.gateway;

import com.mikelcrm.licenseservice.payment.*;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Stripe payment gateway implementation.
 * Uses PaymentIntents API for SCA/3DS compliance (PSD2).
 *
 * Flow:
 * 1. Frontend creates a PaymentMethod (pm_xxx) via Stripe.js
 * 2. Frontend sends pm_xxx to this backend
 * 3. Backend creates PaymentIntent with confirm=true
 * 4. Stripe processes the charge
 * 5. Webhook confirms the final status asynchronously
 */
@Component
@Slf4j
@EnableConfigurationProperties(StripeProperties.class)
@ConditionalOnProperty(name = "license-service.payment.active-gateway", havingValue = "STRIPE")
public class StripePaymentGateway implements PaymentGateway {

    private final StripeProperties stripeProperties;

    public StripePaymentGateway(StripeProperties stripeProperties) {
        this.stripeProperties = stripeProperties;
    }

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeProperties.getApiKey();
        log.info("[Stripe] Gateway inicializado (currency: {})", stripeProperties.getCurrency());
    }

    @Override
    public String getGatewayId() {
        return "STRIPE";
    }

    @Override
    public PaymentResponse processPayment(PaymentRequest request) {
        try {
            // Convert amount to cents (Stripe uses smallest currency unit)
            long amountInCents = request.getAmount().movePointRight(2).longValueExact();

            PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(stripeProperties.getCurrency())
                    .setDescription(request.getDescription() != null ? request.getDescription() : "Mikel CRM Payment")
                    .setConfirm(true)
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                                    .build()
                    );

            // Use paymentMethodId if provided (from Stripe.js frontend)
            // Otherwise fall back to card number for legacy compatibility (will be deprecated)
            if (request.getPaymentMethodId() != null && !request.getPaymentMethodId().isBlank()) {
                paramsBuilder.setPaymentMethod(request.getPaymentMethodId());
            }

            PaymentIntent paymentIntent = PaymentIntent.create(paramsBuilder.build());

            log.info("[Stripe] PaymentIntent created: id={}, status={}, amount={}",
                    paymentIntent.getId(), paymentIntent.getStatus(), amountInCents);

            // Map Stripe status to our response
            String status = mapStripeStatus(paymentIntent.getStatus());

            return PaymentResponse.builder()
                    .status(status)
                    .transactionId(UUID.nameUUIDFromBytes(paymentIntent.getId().getBytes()))
                    .message(status.equals("APPROVED")
                            ? "Pago procesado exitosamente"
                            : "Estado del pago: " + paymentIntent.getStatus())
                    .gatewayId(getGatewayId())
                    .build();

        } catch (StripeException e) {
            log.error("[Stripe] Error procesando pago: code={}, message={}", e.getCode(), e.getMessage());

            return PaymentResponse.builder()
                    .status("REJECTED")
                    .transactionId(UUID.randomUUID())
                    .message(mapStripeError(e))
                    .gatewayId(getGatewayId())
                    .build();

        } catch (Exception e) {
            log.error("[Stripe] Error inesperado: {}", e.getMessage(), e);

            return PaymentResponse.builder()
                    .status("ERROR")
                    .transactionId(UUID.randomUUID())
                    .message("Error interno al procesar el pago")
                    .gatewayId(getGatewayId())
                    .build();
        }
    }

    private String mapStripeStatus(String stripeStatus) {
        return switch (stripeStatus) {
            case "succeeded" -> "APPROVED";
            case "requires_action", "requires_confirmation" -> "PENDING_3DS";
            case "requires_payment_method" -> "REJECTED";
            case "canceled" -> "REJECTED";
            default -> "PENDING";
        };
    }

    private String mapStripeError(StripeException e) {
        if (e.getCode() == null) return "Error de comunicación con Stripe";
        return switch (e.getCode()) {
            case "card_declined" -> "Tarjeta rechazada";
            case "expired_card" -> "Tarjeta expirada";
            case "incorrect_cvc" -> "CVC incorrecto";
            case "insufficient_funds" -> "Fondos insuficientes";
            case "processing_error" -> "Error al procesar — intente de nuevo";
            default -> "Error: " + e.getMessage();
        };
    }
}
