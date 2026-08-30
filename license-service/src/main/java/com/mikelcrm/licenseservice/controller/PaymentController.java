package com.mikelcrm.licenseservice.controller;

import com.mikelcrm.licenseservice.payment.*;
import com.mikelcrm.licenseservice.security.rbac.RequiresPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentGatewayFactory gatewayFactory;

    /**
     * Process a payment — requires authentication (admin role).
     */
    @PostMapping("/process")
    @RequiresPermission(recurso = "contratos", accion = "crear")
    public ResponseEntity<PaymentResponse> processPayment(@Valid @RequestBody PaymentRequest request) {
        PaymentGateway gateway = gatewayFactory.getActiveGateway();
        PaymentResponse response = gateway.processPayment(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Webhook callback from payment gateway — public but should verify signature.
     * TODO: Implement webhook signature verification (e.g., Stripe-Signature header).
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody String payload,
                                                 @RequestHeader(value = "X-Webhook-Signature", required = false) String signature) {
        // TODO: Verify webhook signature before processing
        // if (!verifyWebhookSignature(payload, signature)) { return ResponseEntity.status(403).build(); }
        return ResponseEntity.ok("received");
    }
}
